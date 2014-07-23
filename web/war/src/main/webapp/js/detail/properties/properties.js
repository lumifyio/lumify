
define([
    'flight/lib/component',
    'service/ontology',
    'service/vertex',
    'service/relationship',
    'service/audit',
    'service/config',
    'util/vertex/formatters',
    'util/privileges',
    '../dropdowns/propertyForm/propForm',
    'hbs!../audit/audit-list',
    'data',
    'sf',
    'd3'
], function(
    defineComponent,
    OntologyService,
    VertexService,
    RelationshipService,
    AuditService,
    ConfigService,
    F,
    Privileges,
    PropertyForm,
    auditsListTemplate,
    appData,
    sf,
    d3) {
    'use strict';

    var component = defineComponent(Properties),
        VISIBILITY_NAME = 'http://lumify.io#visibilityJson',
        AUDIT_DATE_DISPLAY = ['date-relative', 'date'],
        AUDIT_DATE_DISPLAY_RELATIVE = 0,
        AUDIT_DATE_DISPLAY_REAL = 1,
        MAX_AUDIT_ITEMS = 3,
        CURRENT_DATE_DISPLAY = AUDIT_DATE_DISPLAY_RELATIVE,
        alreadyWarnedAboutMissingOntology = {},
        ontologyService = new OntologyService(),
        vertexService = new VertexService(),
        relationshipService = new RelationshipService(),
        auditService = new AuditService(),
        configService = new ConfigService();

    return component;

    function isVisibility(property) {
        return property.name === VISIBILITY_NAME;
    }

    function isJustification(property) {
        return (
            property.name === '_justificationMetadata' ||
            property.name === '_sourceMetadata'
        );
    }

    function Properties() {

        this.defaultAttrs({
            addNewPropertiesSelector: '.add-new-properties',
            entityAuditsSelector: '.entity_audit_events',
            auditShowAllSelector: '.show-all-button-row button',
            auditDateSelector: '.audit-date',
            auditUserSelector: '.audit-user',
            auditEntitySelector: '.resolved',
            propertiesInfoSelector: 'button.info',
            showMorePropertiesSelector: '.show-more button'
        });

        this.showPropertyInfo = function(button, property) {
            var vertexId = this.attr.data.id,
                $target = $(button),
                shouldOpen = $target.lookupAllComponents().length === 0;

            require(['util/popovers/propertyInfo/propertyInfo'], function(PropertyInfo) {
                if (shouldOpen) {
                    PropertyInfo.attachTo($target, {
                        property: property,
                        vertexId: vertexId
                    });
                } else {
                    $target.teardownComponent(PropertyInfo);
                }
            });
        };

        this.update = function(properties) {
            var self = this,
                model = self.attr.data,
                isEdge = F.vertex.isEdge(model),
                displayProperties = _.chain(properties)
                    .filter(function(property) {
                        if (isEdge && isJustification(property)) {
                            $.extend(property, {
                                hideInfo: true,
                                hideVisibility: true,
                                displayName: 'Justification',
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

                        if (isEdge && model.label) {
                            var ontologyRelationship = self.ontologyRelationships.byTitle[model.label];
                            properties.push({
                                name: 'relationshipLabel',
                                displayName: 'Type',
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

                        if (isEdge) {
                            return property.name === 'relationshipLabel' ?
                                '1' :
                                isJustification(property) ?
                                '2' : '3';
                        }

                        var ontologyProperty = self.ontologyProperties.byTitle[property.name];
                        if (ontologyProperty && ontologyProperty.displayName) {
                            return '1' + ontologyProperty.displayName.toLowerCase();
                        }
                        return '2' + propertyName.toLowerCase();
                    })
                    .value(),
                row = this.tableRoot.selectAll('tr.property-row')
                    .data(displayProperties)
                    .call(function() {
                        this.enter()
                            .insert('tr', '.buttons-row')
                            .attr('class', 'property-row ')
                            .call(function() {
                                this.append('td')
                                    .attr('class', 'property-name')
                                    .attr('width', '40%')
                                    .append('strong')

                                this.append('td')
                                    .attr('class', 'property-value')
                                    .attr('colspan', 2)
                                    .call(function() {
                                        this.append('span').attr('class', 'value');
                                        this.append('button')
                                            .attr('class', 'info')
                                        this.append('span').attr('class', 'visibility');
                                    })
                            });
                        this.select('button.info')
                            .on('click', function(property) {
                                d3.event.stopPropagation();
                                d3.event.preventDefault();
                                self.showPropertyInfo(this, property);
                            });
                    });

            row.each(function(d) {
                $(this).removePrefixedClasses('property-row-')
                    .addClass('property-row-' + F.className.to(d.name + d.key));
            });

            row.select('td.property-name strong')
                .text(function(d, index) {
                    if (index > 0 && displayProperties[index - 1].name === d.name) {
                        return '';
                    }

                    if (isVisibility(d)) {
                        return 'Visibility';
                    }

                    var ontologyProperty = self.ontologyProperties.byTitle[d.name];
                    if (ontologyProperty) {
                        return ontologyProperty.displayName;
                    }

                    if (d.displayName) {
                        return d.displayName;
                    }

                    console.warn('No ontology definition for ', d.name);
                    return d.name;
                });

            row.select('td.property-value')
                .each(function(property) {
                    var valueSpan = d3.select(this).select('.value').node(),
                        visibilitySpan = d3.select(this).select('.visibility').node(),
                        visibility = isVisibility(property),
                        ontologyProperty = self.ontologyProperties.byTitle[property.name],
                        dataType = ontologyProperty && ontologyProperty.dataType;

                    valueSpan.textContent = '';
                    visibilitySpan.textContent = '';

                    if (visibility) {
                        dataType = 'visibility';
                    } else if (property.hideVisibility !== true) {
                        F.vertex.properties.visibility(
                            visibilitySpan, { value: property[VISIBILITY_NAME] }, self.attr.data.id);
                    }

                    $(this).find('button').toggle(Boolean(
                        !property.hideInfo &&
                        (Privileges.canEDIT || F.vertex.hasMetadata(property))
                    ));

                    if (dataType && F.vertex.properties[dataType]) {
                        F.vertex.properties[dataType](valueSpan, property, self.attr.data.id);
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

            row.exit().remove()
        };

        this.after('initialize', function() {
            var self = this,
                properties = this.attr.data.properties,
                node = this.node,
                root = d3.select(node);

            root.append('div').attr('class', 'entity_audit_events');

            this.tableRoot = root
                .append('table')
                .attr('class', 'table')
                .call(function() {
                    if (!F.vertex.isEdge(self.attr.data)) {
                        this.append('tr')
                            .attr('class', 'buttons-row requires-EDIT')
                            .append('td')
                                .attr('colspan', 3)
                                .attr('class', 'buttons')
                                .append('button')
                                    .attr('class', 'add-new-properties btn btn-mini btn-default')
                                    .text('Add Property');
                    }
                });

            $.when(
                ontologyService.relationships(),
                ontologyService.properties(),
                configService.getProperties()
            ).done(function(ontologyRelationships, ontologyProperties, config) {
                    self.ontologyProperties = ontologyProperties;
                    self.ontologyRelationships = ontologyRelationships;
                    self.update(properties);
            });

            this.on('click', {
                addNewPropertiesSelector: this.onAddNewPropertiesClicked,
                auditDateSelector: this.onAuditDateClicked,
                auditUserSelector: this.onAuditUserClicked,
                auditShowAllSelector: this.onAuditShowAll,
                auditEntitySelector: this.onEntitySelected,
                showMorePropertiesSelector: this.onShowMoreProperties
            });
            this.on('addProperty', this.onAddProperty);
            this.on('deleteProperty', this.onDeleteProperty);
            this.on('editProperty', this.onEditProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

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

        this.before('teardown', function() {
            if (this.auditRequest && this.auditRequest.abort) {
                this.auditRequest.abort();
            }
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

                var vertexId = info.graphVertexId,
                    vertex = appData.vertex(vertexId);
                if (!vertex) {
                    appData.refresh(vertexId).done(function(v) {
                        self.trigger('selectObjects', { vertices: [v] });
                    });
                } else {
                    this.trigger('selectObjects', { vertices: [vertex] });
                }
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

                $.when(
                        ontologyService.ontology(),
                        this.auditRequest = auditService.getAudits(this.attr.data.id)
                    ).done(function(ontology, auditResponse) {
                        var audits = _.sortBy(auditResponse[0].auditHistory, function(a) {
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
                                        [a.relationshipAudit.sourceId], appData.workspaceId);
                                    a.relationshipAudit.sourceInfo =
                                        self.createInfoJsonFromAudit(a.relationshipAudit, 'source');

                                    a.relationshipAudit.destInfo =
                                        self.createInfoJsonFromAudit(a.relationshipAudit, 'dest');
                                    a.relationshipAudit.destHref = F.vertexUrl.fragmentUrl(
                                        [a.relationshipAudit.destId], appData.workspaceId);
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

        this.onShowMoreProperties = function(event) {
            $(event.target)
                .closest('tr')
                    .nextUntil(':not(.hidden)')
                        .removeClass('hidden')
                    .end()
                .remove();
        };

        this.onVerticesUpdated = function(event, data) {
            var self = this;

            data.vertices.forEach(function(vertex) {
                if (vertex.id === self.attr.data.id) {
                    self.attr.data.properties = vertex.properties;
                    self.update(vertex.properties)
                    //self.displayProperties(vertex);
                }
            });
        };

        this.onDeleteProperty = function(event, data) {
            var self = this;

            vertexService.deleteProperty(this.attr.data.id, data.property)
                .fail(this.requestFailure.bind(this, event.target))
        };

        this.onAddProperty = function(event, data) {
            if (data.property.name === 'http://lumify.io#visibilityJson') {

                vertexService.setVisibility(
                        this.attr.data.id,
                        data.property.visibilitySource)
                    .fail(this.requestFailure.bind(this))
                    .done(this.closePropertyForm.bind(this));

            } else {

                vertexService.setProperty(
                        this.attr.data.id,
                        data.property.key,
                        data.property.name,
                        data.property.value,
                        data.property.visibilitySource,
                        data.property.justificationText,
                        data.property.sourceInfo,
                        data.property.metadata)
                    .fail(this.requestFailure.bind(this))
                    .done(this.closePropertyForm.bind(this));
            }

        };

        this.closePropertyForm = function() {
            this.$node.find('.underneath').teardownComponent(PropertyForm);
        };

        this.requestFailure = function(request, message, error) {
            var target = this.$node.find('.underneath');
            if (_.isElement(request)) {
                target = request;
                request = arguments[1];
                message = arguments[2];
                error = arguments[3];
            }

            try {
                error = JSON.parse(error);
            } catch(e) { }

            this.trigger(target, 'propertyerror', { error: error });
        };

        this.onAddNewPropertiesClicked = function(evt) {
            this.trigger('editProperty');
        };

        this.onEditProperty = function(evt, data) {
            var button = this.select('addNewPropertiesSelector'),
                root = $('<div class="underneath">'),
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
                root.insertAfter(button);
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
});
