
define([
    'flight/lib/component',
    '../dropdowns/propertyForm/propForm',
    'util/vertex/formatters',
    'util/privileges',
    'util/withDataRequest',
    'util/popovers/propertyInfo/withPropertyInfo',
    'hbs!../audit/audit-list',
    'd3'
], function(
    defineComponent,
    PropertyForm,
    F,
    Privileges,
    withDataRequest,
    withPropertyInfo,
    auditsListTemplate,
    d3) {
    'use strict';

    var component = defineComponent(Properties, withDataRequest, withPropertyInfo),
        HIDE_PROPERTIES = ['http://lumify.io/comment#entry'],
        VISIBILITY_NAME = 'http://lumify.io#visibilityJson',
        SANDBOX_STATUS_NAME = 'http://lumify.io#sandboxStatus',
        AUDIT_DATE_DISPLAY = ['date-relative', 'date'],
        AUDIT_DATE_DISPLAY_RELATIVE = 0,
        AUDIT_DATE_DISPLAY_REAL = 1,
        MAX_AUDIT_ITEMS = 3,
        CURRENT_DATE_DISPLAY = AUDIT_DATE_DISPLAY_RELATIVE,
        NO_GROUP = '${NO_GROUP}',

        // Property td types
        GROUP = 0, NAME = 1, VALUE = 2,

        alreadyWarnedAboutMissingOntology = {};

    return component;

    function isVisibility(property) {
        return property.name === VISIBILITY_NAME;
    }

    function isSandboxStatus(property) {
        return property.name === SANDBOX_STATUS_NAME;
    }

    function isJustification(property) {
        return (
            property.name === '_justificationMetadata' ||
            property.name === '_sourceMetadata'
        );
    }

    function Properties() {

        this.defaultAttrs({
            entityAuditsSelector: '.entity_audit_events',
            auditShowAllSelector: '.show-all-button-row button',
            auditDateSelector: '.audit-date',
            auditUserSelector: '.audit-user',
            auditEntitySelector: '.resolved',
            propertiesInfoSelector: 'button.info'
        });

        this.update = function(properties) {
            var self = this,
                displayProperties = this.transformPropertiesForUpdate(properties);

            this.reload = this.update.bind(this, properties);

            this.tableRoot.selectAll('tbody.property-group')
                .data(displayProperties)
                .call(
                    _.partial(
                        createPropertyGroups,
                        self.attr.data.id,
                        self.ontologyProperties,
                        self.showMoreExpanded,
                        parseInt(self.config['properties.multivalue.defaultVisibleCount'], 10)
                    )
                );
        };

        this.transformPropertiesForUpdate = function(properties) {
            var self = this,
                model = self.attr.data,
                isEdge = F.vertex.isEdge(model),
                displayProperties = _.chain(properties)
                    .filter(function(property) {
                        if (isEdge && isJustification(property)) {
                            $.extend(property, {
                                hideInfo: true,
                                hideVisibility: true,
                                displayName: i18n('justification.field.label'),
                                justificationData: {
                                    justificationMetadata: property.name === '_justificationMetadata' ?
                                        property.value : null,
                                    sourceMetadata: property.name === '_sourceMetadata' ?
                                        property.value : null
                                }
                            });
                            return true;
                        }

                        if (isVisibility(property)) {
                            return true;
                        }

                        if (~HIDE_PROPERTIES.indexOf(property.name)) {
                            return false;
                        }

                        var ontologyProperty = self.ontologyProperties.byTitle[property.name];
                        return ontologyProperty && ontologyProperty.userVisible;
                    })
                    .tap(function(properties) {
                        var visibility = _.find(properties, isVisibility);
                        if (!visibility) {
                            properties.push({
                                name: VISIBILITY_NAME,
                                value: self.attr.data[VISIBILITY_NAME]
                            });
                        }

                        var sandboxStatus = F.vertex.sandboxStatus(self.attr.data);
                        if (sandboxStatus) {
                            properties.push({
                                name: SANDBOX_STATUS_NAME,
                                displayName: i18n('detail.entity.sandboxStatus'),
                                value: sandboxStatus,
                                hideInfo: true,
                                hideVisibility: true,
                            });
                        }

                        if (isEdge && model.label) {
                            var ontologyRelationship = self.ontologyRelationships.byTitle[model.label];
                            properties.push({
                                name: 'relationshipLabel',
                                displayName: i18n('detail.edge.type'),
                                hideInfo: true,
                                hideVisibility: true,
                                value: ontologyRelationship ?
                                    ontologyRelationship.displayName :
                                    model.label
                            });
                        }
                    })
                    .sortBy(function(property) {
                        if (isVisibility(property)) {
                            return '0';
                        }

                        if (isSandboxStatus(property)) {
                            return '1';
                        }

                        if (isEdge) {
                            return property.name === 'relationshipLabel' ?
                                '2' :
                                isJustification(property) ?
                                '3' : '4';
                        }

                        var ontologyProperty = self.ontologyProperties.byTitle[property.name];
                        if (ontologyProperty && ontologyProperty.propertyGroup) {
                            return '4' + ontologyProperty.propertyGroup.toLowerCase() + ontologyProperty.displayName;
                        }
                        if (ontologyProperty && ontologyProperty.displayName) {
                            return '2' + ontologyProperty.displayName.toLowerCase();
                        }
                        return '3' + property.name.toLowerCase();
                    })
                    .groupBy('name')
                    .pairs()
                    .groupBy(function(pair) {
                        var ontologyProperty = self.ontologyProperties.byTitle[pair[0]];
                        if (ontologyProperty && ontologyProperty.propertyGroup) {
                            return ontologyProperty.propertyGroup;
                        }

                        return NO_GROUP;
                    })
                    .pairs()
                    .value();

            return displayProperties;
        };

        this.after('initialize', function() {
            var self = this,
                properties = this.attr.data.properties,
                node = this.node,
                root = d3.select(node);

            root.append('div').attr('class', 'entity_audit_events');

            this.showMoreExpanded = {};
            this.tableRoot = root
                .append('table')
                .attr('class', 'table')
                .on('click', onTableClick.bind(this));

            Promise.all([
                this.dataRequest('ontology', 'relationships'),
                this.dataRequest('ontology', 'properties'),
                this.dataRequest('config', 'properties')
            ]).done(function(results) {
                var ontologyRelationships = results[0],
                    ontologyProperties = results[1],
                    config = results[2];

                self.config = config;
                self.ontologyProperties = ontologyProperties;
                self.ontologyRelationships = ontologyRelationships;
                self.update(properties);
            });

            this.on('click', {
                auditDateSelector: this.onAuditDateClicked,
                auditUserSelector: this.onAuditUserClicked,
                auditShowAllSelector: this.onAuditShowAll,
                auditEntitySelector: this.onEntitySelected
            });
            this.on('addProperty', this.onAddProperty);
            this.on('deleteProperty', this.onDeleteProperty);
            this.on('editProperty', this.onEditProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'edgesUpdated', this.onEdgesUpdated);

            var positionPopovers = _.throttle(function() {
                    self.trigger('positionPropertyInfo');
                }, 1000 / 60),
                scrollParent = this.$node.scrollParent();

            this.on(document, 'graphPaddingUpdated', positionPopovers);
            if (scrollParent.length) {
                this.on(scrollParent, 'scroll', positionPopovers);
            }

            this.$node
                .closest('.type-content')
                .off('.properties')
                .on('toggleAuditDisplay.properties', this.onToggleAuditing.bind(this));
        });

        this.onAuditShowAll = function(event) {
            var row = $(event.target).closest('tr');

            row.prevUntil('.property').removeClass('hidden');
            row.remove();
        };

        this.onEntitySelected = function(event) {
            var self = this,
                $target = $(event.target),
                info = $target.data('info');

            if (info) {
                event.preventDefault();

                var vertexId = info.graphVertexId;

                this.trigger('selectObjects', { vertexIds: [vertexId] });
            }
        };

        this.onAuditDateClicked = function(event) {
            CURRENT_DATE_DISPLAY = (CURRENT_DATE_DISPLAY + 1) % AUDIT_DATE_DISPLAY.length;

            this.$node.find('.audit-date').each(function() {
                $(this).text($(this).data(AUDIT_DATE_DISPLAY[CURRENT_DATE_DISPLAY]));
            });
        };

        this.onAuditUserClicked = function(event) {
            var userId = $(event.target).data('userId');
            if (userId) {
                this.trigger('selectUser', { userId: userId });
            }
        };

        this.onToggleAuditing = function(event, data) {
            var self = this,
                auditsEl = this.select('entityAuditsSelector');

            if (data.displayed) {
                auditsEl.html('<div class="nav-header">Audits<span class="badge loading"/></div>').show();
                this.$node
                    .find('.audit-list').remove().end()
                    .find('.hidden').removeClass('hidden').end()
                    .find('.show-more').remove();

                Promise.all([
                    this.dataRequest('ontology', 'ontology'),
                    this.dataRequest(
                        F.vertex.isEdge(this.attr.data) ? 'edge' : 'vertex',
                        'audit',
                        this.attr.data.id
                    )
                ]).done(function(results) {
                    var ontology = results[0],
                        auditHistory = results[1],
                        audits = _.sortBy(auditHistory, function(a) {
                            return new Date(a.dateTime).getTime() * -1;
                        }),
                        auditGroups = _.groupBy(audits, function(a) {
                            if (a.entityAudit) {
                               if (a.entityAudit.analyzedBy) {
                                   a.data.displayType = a.entityAudit.analyzedBy;
                               }
                            }

                            if (a.propertyAudit) {
                                a.propertyAudit.isVisibility =
                                    a.propertyAudit.propertyName === 'http://lumify.io#visibilityJson';
                                a.propertyAudit.visibilityValue = a.propertyAudit.propertyMetadata &&
                                    a.propertyAudit.propertyMetadata['http://lumify.io#visibilityJson'];
                                a.propertyAudit.formattedValue = F.vertex.displayProp({
                                    name: a.propertyAudit.propertyName,
                                    value: a.propertyAudit.newValue || a.propertyAudit.previousValue
                                });
                                a.propertyAudit.isDeleted = a.propertyAudit.newValue === '';

                                return 'property';
                            }

                            if (a.relationshipAudit) {
                                a.relationshipAudit.sourceIsCurrent =
                                    a.relationshipAudit.sourceId === self.attr.data.id;
                                a.relationshipAudit.sourceHref = F.vertexUrl.fragmentUrl(
                                    [a.relationshipAudit.sourceId], lumifyData.currentWorkspaceId);
                                a.relationshipAudit.sourceInfo =
                                    self.createInfoJsonFromAudit(a.relationshipAudit, 'source');

                                a.relationshipAudit.destInfo =
                                    self.createInfoJsonFromAudit(a.relationshipAudit, 'dest');
                                a.relationshipAudit.destHref = F.vertexUrl.fragmentUrl(
                                    [a.relationshipAudit.destId], lumifyData.currentWorkspaceId);
                            }

                            return 'other';
                        });

                    self.select('entityAuditsSelector')
                        .empty()
                        .append('<table></table>')
                        .find('table')
                        .append(auditsListTemplate({
                            audits: auditGroups.other || [],
                            MAX_TO_DISPLAY: MAX_AUDIT_ITEMS
                        }));

                    if (auditGroups.property) {
                        self.updatePropertyAudits(auditGroups.property);
                    }
                    auditsEl.show();

                    self.trigger('updateDraggables');
                    self.updateVisibility();
                });
            } else {
                auditsEl.hide();
                this.$node.find('.audit-row').remove();
                this.$node.find('.audit-only-property').remove();
                this.$node.find('.show-all-button-row').remove();
            }
        };

        this.updatePropertyAudits = function(audits) {
            var self = this,
                auditsByProperty = _.groupBy(audits, function(a) {
                    return a.propertyAudit.propertyName + a.propertyAudit.propertyKey;
                });

            Object.keys(auditsByProperty).forEach(function(propertyNameAndKey) {
                var propLi = self.$node.find('.property-row-' + F.className.to(propertyNameAndKey)),
                    audits = auditsByProperty[propertyNameAndKey],
                    propertyKey = audits[0].propertyAudit.propertyKey,
                    propertyName = audits[0].propertyAudit.propertyName;

                // TODO: support properties that were deleted
                if (propLi.length) {
                    propLi.after(auditsListTemplate({
                        audits: audits,
                        MAX_TO_DISPLAY: MAX_AUDIT_ITEMS
                    }));
                }
            });
        };

        this.createInfoJsonFromAudit = function(audit, direction) {
            var info;

            if (direction) {
                var type = audit[direction + 'Type'];

                info = {
                    'http://lumify.io#conceptType': audit[direction + 'Type'],
                    title: audit[direction + 'Title'],
                    graphVertexId: audit[direction + 'Id']
                };
            } else {
                info = {
                    _type: audit.type,
                    'http://lumify.io#conceptType': audit.subType,
                    title: audit.title,
                    graphVertexId: audit.id
                };
            }

            return JSON.stringify(info);
        };

        this.onEdgesUpdated = function(event, data) {
            var edge = _.findWhere(data.edges, { id: this.attr.data.id });
            if (edge) {
                this.attr.data = edge;
                this.update(edge.properties);
            }
        };

        this.onVerticesUpdated = function(event, data) {
            var vertex = _.findWhere(data.vertices, { id: this.attr.data.id });
            if (vertex) {
                this.attr.data = vertex;
                this.update(vertex.properties)
            }
        };

        this.onDeleteProperty = function(event, data) {
            var self = this;

            this.dataRequest(
                    F.vertex.isEdge(this.attr.data) ? 'edge' : 'vertex',
                    'deleteProperty',
                    this.attr.data.id, data.property
                ).then(this.closePropertyForm.bind(this))
                 .catch(this.requestFailure.bind(this, event.target))
        };

        this.onAddProperty = function(event, data) {
            if (data.property.name === 'http://lumify.io#visibilityJson') {
                if (data.isEdge) {
                    this.dataRequest('edge', 'setVisibility', this.attr.data.id, data.property.visibilitySource)
                        .then(this.closePropertyForm.bind(this))
                        .catch(this.requestFailure.bind(this))
                } else {
                    this.dataRequest('vertex', 'setVisibility', this.attr.data.id, data.property.visibilitySource)
                        .then(this.closePropertyForm.bind(this))
                        .catch(this.requestFailure.bind(this));
                }
            } else {
                this.dataRequest('vertex', 'setProperty', this.attr.data.id, data.property)
                    .then(this.closePropertyForm.bind(this))
                    .catch(this.requestFailure.bind(this));
            }

        };

        this.closePropertyForm = function() {
            this.$node.find('.underneath').teardownComponent(PropertyForm);
        };

        this.requestFailure = function(error) {
            var target = this.$node.find('.underneath');
            this.trigger(target, 'propertyerror', { error: error });
        };

        this.onEditProperty = function(evt, data) {
            var root = $('<div class="underneath">'),
                property = data && data.property,
                propertyRow = property && $(evt.target).closest('tr')

            this.$node.find('button.info').popover('hide');

            if (propertyRow && propertyRow.length) {
                root.appendTo(
                    $('<tr><td colspan=3></td></tr>')
                        .insertAfter(propertyRow)
                        .find('td')
                );
            } else {
                $('<tr><td colspan="3"></td></tr>').prependTo(this.$node.find('table')).find('td').append(root);
            }

            PropertyForm.teardownAll();
            PropertyForm.attachTo(root, {
                data: this.attr.data,
                property: property
            });
        };

        this.updateJustification = function() {
            this.$node.find('.justification').each(function() {
                var justification = $(this),
                    property = justification.data('property');

                require(['util/vertex/justification/viewer'], function(JustificationViewer) {
                    var attrs = {};
                    attrs[property.name] = property.value;
                    JustificationViewer.attachTo(justification, attrs);
                });
            });
        }

        this.updateVisibility = function() {
            var self = this;

            require([
                'configuration/plugins/visibility/visibilityDisplay'
            ], function(VisibilityDisplay) {
                self.$node.find('.visibility').each(function() {
                    var visibility = $(this).data('visibility');
                    VisibilityDisplay.attachTo(this, {
                        value: visibility && visibility.source
                    })
                });
            });
        };
    }

    function onTableClick() {
        var $target = $(d3.event.target),
            $header = $target.closest('.property-group-header'),
            $tbody = $header.closest('.property-group'),
            processed = true;

        if ($header.is('.property-group-header')) {
            $tbody.toggleClass('collapsed expanded');
        } else if ($target.is('.show-more')) {
            var isShowing = $target.data('showing');
            $target.data('showing', !isShowing);
            if (isShowing) {
                delete this.showMoreExpanded[$target.data('propertyName')];
            } else {
                this.showMoreExpanded[$target.data('propertyName')] = true;
            }
            this.reload();
        } else if ($target.is('.info')) {
            var datum = d3.select($target.closest('.property-value').get(0)).datum();
            this.showPropertyInfo($target, this.attr.data.id, datum.property);
        } else {
            processed = false;
        }

        if (processed) {
            d3.event.stopPropagation();
            d3.event.preventDefault();
        }
    }

    function createPropertyGroups(vertexId, ontologyProperties, showMoreExpanded, maxItemsBeforeHidden) {
        this.enter()
            .insert('tbody', '.buttons-row')
            .attr('class', function(d, groupIndex, j) {
                var cls = 'property-group collapsible';
                if (groupIndex === 0) {
                    return cls + ' expanded';
                }

                return cls + ' collapsed';
            });

        var totalPropertyCountsByName = {};

        this.selectAll('tr.property-group-header, tr.property-row')
            .data(function(pair) {
                return _.chain(pair[1])
                    .map(function(p) {
                        totalPropertyCountsByName[p[0]] = p[1].length - maxItemsBeforeHidden;
                        if (p[0] in showMoreExpanded) {
                            return p[1];
                        }
                        return p[1].slice(0, maxItemsBeforeHidden);
                    })
                    .flatten()
                    .tap(function(list) {
                        if (pair[0] !== NO_GROUP) {
                            list.splice(0, 0, [pair[0], {
                                propertyCount: pair[1].length,
                                valueCount: _.reduce(pair[1], function(sum, p) {
                                    return sum + p[1].length;
                                }, 0)
                            }]);
                        }
                    })
                    .value();
            })
            .call(
                _.partial(createProperties,
                          vertexId,
                          ontologyProperties,
                          totalPropertyCountsByName,
                          maxItemsBeforeHidden,
                          showMoreExpanded
                )
            )

        this.exit().remove();
    }

    function createProperties(vertexId,
                              ontologyProperties,
                              totalPropertyCountsByName,
                              maxItemsBeforeHidden,
                              showMoreExpanded) {

        this.enter()
            .append('tr')
            .attr('class', function(datum) {
                if (_.isString(datum[0])) {
                    return 'property-group-header';
                }
                return 'property-row property-row-' + F.className.to(datum.name + datum.key);
            });

        var currentPropertyIndex = 0, lastPropertyName = '';
        this.selectAll('td')
            .data(function(datum, i, j) {
                if (_.isString(datum[0])) {
                    return [{
                        type: GROUP,
                        name: datum[0],
                        count: datum[1]
                    }];
                }

                if (datum.name === lastPropertyName) {
                    currentPropertyIndex++;
                } else {
                    currentPropertyIndex = 0;
                    lastPropertyName = datum.name;
                }

                return [
                    {
                        type: NAME,
                        name: datum.name,
                        property: datum
                    },
                    {
                        type: VALUE,
                        property: datum,
                        propertyIndex: currentPropertyIndex,
                        showToggleLink: currentPropertyIndex === (maxItemsBeforeHidden - 1),
                        isExpanded: datum.name in showMoreExpanded,
                        hidden: Math.max(0, totalPropertyCountsByName[datum.name])
                    }
                ];
            })
            .call(_.partial(createPropertyRow, vertexId, ontologyProperties, maxItemsBeforeHidden));

        this.exit().remove();
    }

    function createPropertyRow(vertexId, ontologyProperties, maxItemsBeforeHidden) {
        this.enter()
            .append('td')
            .each(function() {
                var self = d3.select(this),
                    datum = self.datum();
                switch (datum.type) {
                    case GROUP:
                        self.append('h1')
                            .attr('class', 'collapsible-header')
                            .call(function() {
                                this.append('span').attr('class', 'badge');
                                this.append('strong');
                            });
                            break;
                    case NAME: self.append('strong'); break;
                    case VALUE:
                        self.append('span').attr('class', 'value');
                        self.append('button').attr('class', 'info')
                        self.append('span').attr('class', 'visibility');
                        if (datum.propertyIndex === (maxItemsBeforeHidden - 1)) {
                            self.append('a').attr('class', 'show-more');
                        }
                        break;
                }
            });

        this.attr('class', function(datum) {
                if (datum.type === NAME) {
                    return 'property-name';
                } else if (datum.type === VALUE) {
                    return 'property-value';
                }
            })
            .attr('width', function(datum) {
                if (datum.type === NAME) {
                    return '40%';
                }
            })
            .attr('colspan', function(datum) {
                if (datum.type === GROUP) {
                    return '3';
                } else if (datum.type === VALUE) {
                    return '2';
                }
                return '1';
            })
            .call(function() {
                var previousPropertyName = '';

                this.select('h1.collapsible-header strong').text(_.property('name'))
                this.select('h1.collapsible-header .badge')
                    .text(function(d) {
                        return i18n('properties.groups.count',
                            F.number.pretty(d.count.propertyCount),
                            F.number.pretty(d.count.valueCount)
                        );
                    })
                    .attr('title', function(d) {
                        var propertyLabel = 'properties.groups.count.hover.property',
                            valueLabel = 'properties.groups.count.hover.value';

                        if (d.count.propertyCount > 1) {
                            propertyLabel += '.plural';
                        }
                        if (d.count.valueCount > 1) {
                            valueLabel += '.plural';
                        }

                        return [
                            F.number.pretty(d.count.propertyCount),
                            i18n(propertyLabel),
                            F.number.pretty(d.count.valueCount),
                            i18n(valueLabel)
                        ].join(' ')
                    });

                this.select('.property-name strong')
                    .text(function(d) {
                        if (previousPropertyName === d.name) {
                            return '';
                        }
                        previousPropertyName = d.name;

                        if (isVisibility(d)) {
                            return i18n('visibility.label');
                        }

                        var ontologyProperty = ontologyProperties.byTitle[d.name];
                        if (ontologyProperty) {
                            return ontologyProperty.displayName;
                        }

                        if (d.property.displayName) {
                            return d.property.displayName;
                        }

                        console.warn('No ontology definition for ', d.name);
                        return d.name;
                    });

                this.select('.property-value .value')
                    .each(function() {
                        var self = d3.select(this),
                            property = self.datum().property,
                            valueSpan = self.node(),
                            $valueSpan = $(valueSpan),
                            visibilitySpan = $valueSpan.siblings('.visibility')[0],
                            $infoButton = $valueSpan.siblings('.info'),
                            visibility = isVisibility(property),
                            ontologyProperty = ontologyProperties.byTitle[property.name],
                            dataType = ontologyProperty && ontologyProperty.dataType,
                            displayType = ontologyProperty && ontologyProperty.displayType;

                        valueSpan.textContent = '';
                        visibilitySpan.textContent = '';

                        if (visibility) {
                            dataType = 'visibility';
                        } else if (property.hideVisibility !== true) {
                            F.vertex.properties.visibility(
                                visibilitySpan,
                                { value: property.metadata && property.metadata[VISIBILITY_NAME] },
                                vertexId);
                        }

                        $infoButton.toggle(Boolean(
                            !property.hideInfo &&
                            (Privileges.canEDIT || F.vertex.hasMetadata(property))
                        ));

                        if (displayType && F.vertex.properties[displayType]) {
                            F.vertex.properties[displayType](valueSpan, property, vertexId);
                            return;
                        } else if (dataType && F.vertex.properties[dataType]) {
                            F.vertex.properties[dataType](valueSpan, property, vertexId);
                            return;
                        }

                        if (isJustification(property)) {
                            require(['util/vertex/justification/viewer'], function(JustificationViewer) {
                                $(valueSpan).teardownAllComponents();
                                JustificationViewer.attachTo(valueSpan, property.justificationData);
                            });
                            return;
                        }

                        valueSpan.textContent = F.vertex.displayProp(property);
                    });

                this.select('.property-value .show-more')
                    .attr('data-property-name', function(d) {
                        return d.property.name;
                    })
                    .text(function(d) {
                        return i18n(
                            'properties.button.' + (d.isExpanded ? 'hide_more' : 'show_more'),
                            F.number.pretty(d.hidden),
                            ontologyProperties.byTitle[d.property.name].displayName
                        );
                    })
                    .style('display', function(d) {
                        if (d.showToggleLink && d.hidden > 0) {
                            return 'block';
                        }

                        return 'none';
                    });
            })

        this.exit().remove();
    }

});
