
define([
    'flight/lib/component',
    'service/ontology',
    'service/vertex',
    'service/relationship',
    'service/audit',
    'util/formatters',
    './dropdowns/propertyForm/propForm',
    'tpl!./properties',
    'tpl!./propertiesItem',
    'tpl!./audit-list',
    'data',
    'sf'
], function(
    defineComponent,
    OntologyService,
    VertexService,
    RelationshipService,
    AuditService,
    formatters,
    PropertyForm,
    propertiesTemplate,
    propertiesItemTemplate,
    auditsListTemplate,
    appData,
    sf) {
    'use strict';

    var component = defineComponent(Properties),
        AUDIT_DATE_DISPLAY = ['date-relative', 'date'],
        AUDIT_DATE_DISPLAY_RELATIVE = 0,
        AUDIT_DATE_DISPLAY_REAL = 1,
        MAX_AUDIT_ITEMS = 5,
        CURRENT_DATE_DISPLAY = AUDIT_DATE_DISPLAY_RELATIVE;

    component.filterPropertiesForDisplay = filterPropertiesForDisplay;
    return component;

    function Properties() {

        this.ontologyService = new OntologyService();
        this.vertexService = new VertexService();
        this.relationshipService = new RelationshipService();
        this.auditService = new AuditService();

        this.defaultAttrs({
            addNewPropertiesSelector: '.add-new-properties',
            entityAuditsSelector: '.entity_audit_events',
            auditShowAllSelector: '.audit-list button',
            auditDateSelector: '.audit-date',
            auditUserSelector: '.audit-user',
            auditEntitySelector: '.resolved',
            propertiesInfoSelector: 'button.info'
        });

        this.after('initialize', function() {
            this.on('click', {
                addNewPropertiesSelector: this.onAddNewPropertiesClicked,
                auditDateSelector: this.onAuditDateClicked,
                auditUserSelector: this.onAuditUserClicked,
                auditShowAllSelector: this.onAuditShowAll,
                auditEntitySelector: this.onEntitySelected,
            });
            this.on('addProperty', this.onAddProperty);
            this.on('deleteProperty', this.onDeleteProperty);
            this.on('editProperty', this.onEditProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node
                .closest('.type-content')
                .off('.properties')
                .on('toggleAuditDisplay.properties', this.onToggleAuditing.bind(this));

            this.$node.html(propertiesTemplate({properties: null}));
            this.displayProperties(this.attr.data.properties);
        });

        this.before('teardown', function() {
            if (this.auditRequest && this.auditRequest.abort) {
                this.auditRequest.abort();
            }
        });

        this.onAuditShowAll = function(event) {
            $(event.target).closest('.audit-list').addClass('showAll');
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
                this.trigger('startChat', { userId: userId });
            }
        };

        this.onToggleAuditing = function(event, data) {
            var self = this,
                auditsEl = this.select('entityAuditsSelector');

            if (data.displayed) {
                auditsEl.html('<div class="nav-header">Audits<span class="badge loading"/></div>').show();
                this.$node.find('.audit-list').remove();

                this.auditRequest = this.auditService.getAudits(this.attr.data.id)
                    .done(function(auditResponse) {
                        var audits = _.sortBy(auditResponse.auditHistory, function(a) { 
                                return new Date(a.dateTime).getTime() * -1; 
                            }),
                            auditGroups = _.groupBy(audits, function(a) {
                                return a.propertyAudit ? 'property' : 'other';
                                       //a.relationshipAudit ? 'relation' : 'other';
                            });

                        self.select('entityAuditsSelector').html(auditsListTemplate({
                            audits: auditGroups.other || [],
                            formatters: formatters,
                            formatValue: self.formatValue.bind(self),
                            currentVertexId: self.attr.data.id,
                            createInfoJsonFromAudit: self.createInfoJsonFromAudit.bind(self),
                            MAX_TO_DISPLAY: MAX_AUDIT_ITEMS
                        }));

                        if (auditGroups.property) {
                            self.updatePropertyAudits(auditGroups.property);
                        }
                        auditsEl.show();                        

                        self.trigger('updateDraggables');
                    });
            } else {
                auditsEl.hide();
                this.$node.find('.audit-list').remove();
                this.$node.find('.audit-only-property').remove();
            }
        };

        this.formatValue = function(v, propertyName, audit) {
            var property = this.ontologyProperties.byTitle[propertyName],
                dataType = property && property.dataType || 'string';

            switch (dataType) {
                case 'date': return formatters.date.dateString(v);
                case 'number': return formatters.number.pretty(v);
                case 'geoLocation':
                    var geo = formatters.geoLocation.parse(v);
                    return geo ? (geo.latitude + ',' + geo.longitude) : v;
                default:
                    return v;
            }
            
            //audit.toValue = sf("{0:yyyy/MM/dd}", new Date(audit.toValue + " 00:00"));
        };

        this.updatePropertyAudits = function(audits) {
            var self = this,
                auditsByProperty = _.groupBy(audits, function(a) { 
                    return a.propertyAudit.propertyName; 
                });

            Object.keys(auditsByProperty).forEach(function(propertyName) {
                var propLi = self.$node.find('.property-' + propertyName);
                if (!propLi.length && !(/^_/).test(propertyName)) {
                    var property = self.ontologyProperties.byTitle[propertyName],
                        value;

                    for (var i = 0; i < auditsByProperty[propertyName].length; i++) {
                        var propAudit = auditsByProperty[propertyName][i].propertyAudit;
                        value = propAudit.newValue || propAudit.previousValue;
                        if (value) {
                            break;
                        }
                    }

                    if (property.dataType === 'geoLocation') {
                        value = formatters.geoLocation.parse(value);
                    }

                    propLi = $(
                        propertiesItemTemplate({
                            property: {
                                key: property.title,
                                displayName: property.displayName,
                                value: value || 'deleted',
                                metadata: {}
                            },
                            popout: false
                        })
                    ).addClass('audit-only-property').prependTo(self.$node.find('table tbody'));
                }
                propLi.after('<tr><td colspan=2></td></tr>')
                    .next('tr').find('td')
                    .append(auditsListTemplate({
                        audits: auditsByProperty[propertyName],
                        formatters: formatters,
                        formatValue: self.formatValue.bind(self),
                        currentVertexId: self.attr.data.id,
                        createInfoJsonFromAudit: self.createInfoJsonFromAudit.bind(self),
                        MAX_TO_DISPLAY: MAX_AUDIT_ITEMS
                    }));
            });

            this.updatePopovers();
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

        this.onVerticesUpdated = function(event, data) {
            var self = this;

            data.vertices.forEach(function(vertex) {
                if (vertex.id === self.attr.data.id) {
                    self.displayProperties(vertex.properties);
                }
            });
        };

        this.onDeleteProperty = function(event, data) {
            var self = this;

            if (self.attr.data.properties['http://lumify.io#conceptType'].value === 'relationship') {
                self.relationshipService.deleteProperty(
                        data.property.name,
                        this.attr.data.properties.source.value,
                        this.attr.data.properties.target.value,
                        this.attr.data.id)
                .fail(this.requestFailure.bind(this))
                .done(function(newProperties) {
                    var properties = $.extend({}, self.attr.data.properties, newProperties);
                    self.displayProperties(properties);
                });

            } else {
                this.vertexService.deleteProperty(
                    this.attr.data.id,
                    data.property)
                    .fail(this.requestFailure.bind(this, event.target))
            }
        };

        this.onAddProperty = function(event, data) {
            var self = this,
                isEdge = this.attr.data.properties['http://lumify.io#conceptType'].value === 'relationship',
                done = isEdge ? function(edge) {
                    var properties = $.extend({}, self.attr.data.properties, edge.properties);
                    self.displayProperties(properties);
                } : function() { };

            if (data.property.name === '_visibilityJson') {

                this[isEdge ? 'relationshipService' : 'vertexService'].setVisibility(
                        this.attr.data.id,
                        data.property.visibilitySource)
                    .fail(this.requestFailure.bind(this))
                    .done(done);

            } else if (isEdge) {

                this.relationshipService.setProperty(
                        data.property.name,
                        data.property.value,
                        data.property.visibilitySource,
                        data.property.justificationText,
                        data.property.sourceInfo,
                        this.attr.data.properties.source.value,
                        this.attr.data.properties.target.value,
                        this.attr.data.id)
                    .fail(this.requestFailure.bind(this))
                    .done(done);

            } else {

                this.vertexService.setProperty(
                        this.attr.data.id,
                        data.property.name,
                        data.property.value,
                        data.property.visibilitySource,
                        data.property.justificationText,
                        data.property.sourceInfo)
                    .fail(this.requestFailure.bind(this))
                    .done(done);
            }

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
                propertyRow = property && this.$node.find('.property-' + property.key);

            this.$node.find('button.info').popover('hide');

            if (propertyRow && propertyRow.length) {
                root.appendTo(
                    $('<tr><td colspan=2></td></tr>')
                        .insertAfter(propertyRow)
                        .find('td')
                );
            } else {
                root.insertAfter(button);
            }

            PropertyForm.teardownAll();
            PropertyForm.attachTo(root, {
                service: this.ontologyService,
                data: this.attr.data,
                property: property
            });
        };

        this.onPropertyChange = function(propertyChangeData) {
            if (propertyChangeData.id != this.attr.data.id) {
                return;
            }
            if (propertyChangeData.propertyName == 'title') {
                this.select('titleSelector').html(propertyChangeData.value);
            }
            this.select('propertiesSelector')
                .find('.property-' + propertyChangeData.propertyName + ' .value')
                .html(propertyChangeData.value);
        };

        this.updatePopovers = function() {
            var self = this;

            require(['detail/propertyInfo'], function(PropertyInfo) {

                var infos = self.$node.find('.info');

                infos.each(function() {
                    var $this = $(this),
                    property = $this.data('property'),
                    ontologyProperty = self.ontologyProperties.byTitle[property.key];

                    if (property.key === '_visibilityJson' || ontologyProperty) {
                        $this.popover('destroy');
                        $this.popover({
                            trigger: 'click',
                            placement: 'top',
                            content: 'Loading...',
                            //delay: { show: 100, hide: 1000 }
                        });

                        $this.on('shown', function() {
                            infos.not($this).popover('hide');
                        });

                        var popover = $this.data('popover'),
                        tip = popover.tip(),
                        content = tip.find('.popover-content');

                        popover.setContent = function() {
                            var $tip = this.tip()
                            $tip.removeClass('fade in top bottom left right')
                        };

                        content.teardownAllComponents();
                        PropertyInfo.attachTo(content, { 
                            property: $this.data('property')
                        })
                    } else {
                        $this.remove();
                    }
                })
            })
        }

        this.displayProperties = function(properties) {
            var self = this;

            this.ontologyService.properties()
                .done(function(ontologyProperties) {
                    var filtered = filterPropertiesForDisplay(properties, ontologyProperties),
                        popoutEnabled = false,
                        iconProperty = _.findWhere(filtered, { key: 'http://lumify.io#glyphIcon' });

                    self.ontologyProperties = ontologyProperties;

                    if (iconProperty) {
                        self.trigger(self.select('glyphIconSelector'), 'iconUpdated', { src: iconProperty.value });
                    }

                    if ($('#app').hasClass('fullscreen-details')) {
                        popoutEnabled = true;
                    }

                    require([
                        'configuration/plugins/visibility/visibilityDisplay'
                    ], function(VisibilityDisplay) {
                        var props = $(propertiesTemplate({properties: filtered, popout: popoutEnabled}));

                        props.find('.visibility').each(function() {
                            var visibility = $(this).data('visibility');
                            VisibilityDisplay.attachTo(this, {
                                value: visibility.source
                            })
                        });

                        self.$node.html(props);

                        self.updatePopovers();
                    });
                });
            self.trigger('toggleAuditDisplay', { displayed: false })
        };
    }

    function filterPropertiesForDisplay(properties, ontologyProperties) {
        var displayProperties = [];

        if ($.isArray(properties)) {
            var o = {};
            properties.forEach(function(p) {
                o[p.key] = p.value;
            });
            properties = o;
        }

        if (!('_visibilityJson' in properties)) {
            properties._visibilityJson = {
                value: {
                    source: ''
                }
            };
        }

        var keys = Object.keys(properties).sort(function(a,b) {
            if (a === '_visibilityJson') return -1;
            if (b === '_visibilityJson') return 1;
            if (a === 'startDate' && b === 'endDate') return -1;
            if (b === 'startDate' && a === 'endDate') return 1;

            return a < b ? -1 : a > b ? 1 : 0;
        });

        keys.forEach(function(name) {
            var displayName, value,
                ontologyProperty = ontologyProperties.byTitle[name],
                isEdge = properties['http://lumify.io#conceptType'] && 
                    properties['http://lumify.io#conceptType'].value === 'relationship',
                isRelationshipType = name === 'relationshipType' && isEdge;

            if (ontologyProperty) {
                displayName = ontologyProperty.displayName;

                if (ontologyProperty.dataType == 'date') {
                    value = formatters.date.dateString(parseInt(properties[name].value, 10));
                } else if (ontologyProperty.dataType === 'geoLocation') {
                    value = properties[name];
                    value['http://lumify.io#geoLocationDescription'] = 
                        properties['http://lumify.io#geoLocationDescription'];
                } else {
                    value = properties[name].value;
                }

                if (// Ignore underscore leading property names
                    /^[^_]/.test(name) &&

                    // Showing the source and target for an edge is redundant (shown in title)
                    (!isEdge || !_.contains(['source', 'target'], name)) &&

                    // Bounding box not useful to show
                    name !== 'boundingBox' &&

                    // Title is displayed above property list
                    name !== 'title') {

                    addProperty(properties[name], name, displayName, value, properties[name]._visibilityJson);
                }
            } else if (name === '_visibilityJson') {
                value = properties[name].value;

                var source = (value && value.value && value.value.source) || (value && value.source) || '';

                addProperty(properties[name], name, 'Visibility', source);
            } else if (isRelationshipType) {
                addProperty(properties[name], name, 'Relationship type', properties[name].value);
            }
        });
        return displayProperties;

        function addProperty(property, name, displayName, value, visibility) {
            displayProperties.push({
                key: name,
                value: value,
                displayName: displayName || name,
                visibility: visibility,
                metadata: _.pick(property, 'sandboxStatus', '_justificationMetadata', '_sourceMetadata')
            });
        }
    }
});
