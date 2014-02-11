
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
], function (
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
            auditEntitySelector: '.resolved'
        });

        this.after('initialize', function () {
            this.on('click', {
                addNewPropertiesSelector: this.onAddNewPropertiesClicked,
                auditDateSelector: this.onAuditDateClicked,
                auditUserSelector: this.onAuditUserClicked,
                auditShowAllSelector: this.onAuditShowAll,
                auditEntitySelector: this.onEntitySelected
            });
            this.on('addProperty', this.onAddProperty);
            this.on('deleteProperty', this.onDeleteProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node
                .closest('.type-content')
                .off('.properties')
                .on('toggleAuditDisplay.properties', this.onToggleAuditing.bind(this));

            this.$node.html(propertiesTemplate({properties:null}));
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
                        self.trigger('selectObjects', { vertices:[v] });
                    });
                } else {
                    this.trigger('selectObjects', { vertices:[vertex] });
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
                this.trigger('startChat', { userId:userId });
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
                                value: value || 'deleted'
                            },
                            popout: false
                        })
                    ).addClass('audit-only-property').insertBefore(self.$node.find('ul .buttons'));
                }
                propLi.append(auditsListTemplate({
                    audits: auditsByProperty[propertyName],
                    formatters: formatters,
                    formatValue: self.formatValue.bind(self),
                    currentVertexId: self.attr.data.id,
                    createInfoJsonFromAudit: self.createInfoJsonFromAudit.bind(self),
                    MAX_TO_DISPLAY: MAX_AUDIT_ITEMS
                }));
            });
        };

        this.createInfoJsonFromAudit = function(audit, direction) {
            var info;

            if (direction) {
                var type = audit[direction + 'Type'];

                info = {
                    _conceptType: audit[direction + 'Type'],
                    title: audit[direction + 'Title'],
                    graphVertexId: audit[direction + 'Id']
                };
            } else {
                info = {
                    _type: audit.type,
                    _conceptType: audit.subType,
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

            if (self.attr.data.properties._type) {
                self.relationshipService.deleteProperty(
                        data.property.name,
                        this.attr.data.properties.source.value,
                        this.attr.data.properties.target.value,
                        this.attr.data.id)
                .fail(this.requestFailure.bind(this))
                .done(function(newProperties) {
                    var properties = $.extend({}, self.attr.data.properties, newProperties);
                    self.displayProperties(properties);
                    self.trigger('updateRelationships', [{
                        id: self.attr.data.id,
                        properties: properties
                    }]);
                });

            } else {
                this.vertexService.deleteProperty(
                    this.attr.data.id,
                    data.property)
                    .fail(this.requestFailure.bind(this))
                    .done(function(vertexData) {
                        self.displayProperties(vertexData.properties);
                        self.trigger (document, "updateVertices", { 
                            vertices: [{
                                id: vertexData.graphVertexId,
                                properties: vertexData.properties
                            }]
                        });
                    });
            }
        };

        this.onAddProperty = function (event, data) {
            var self = this;

            if (self.attr.data.properties._type && self.attr.data.properties._type === 'relationship') {
                self.relationshipService.setProperty(
                        data.property.name,
                        data.property.value,
                        this.attr.data.properties.source.value,
                        this.attr.data.properties.target.value,
                        this.attr.data.properties.id.value)
                .fail(this.requestFailure.bind(this))
                .done(function(newProperties) {
                    var properties = $.extend({}, self.attr.data.properties, newProperties);
                    self.displayProperties(properties);
                    self.trigger('updateRelationships', [{
                        id: self.attr.data.id,
                        properties: properties
                    }]);
                });
            } else {
                self.vertexService.setProperty(
                    this.attr.data.id,
                    data.property.name,
                    data.property.value,
                    data.property.visibilitySource)
                    .fail(this.requestFailure.bind(this))
                    .done(function(vertexData) {
                        self.displayProperties(vertexData.properties);
                        self.trigger (document, "updateVertices", { 
                            vertices: [{
                                id: vertexData.graphVertexId,
                                properties: vertexData.properties
                            }]
                        });
                    });
            }

        };
        
        this.requestFailure = function(err) {
            if (err.status == 400) {
                console.error('Validation error');
                return this.trigger(this.$node.find('.underneath'), 'addPropertyError', {});
            }
            return 0;
        };

        this.onAddNewPropertiesClicked = function (evt) {
            var root = $('<div class="underneath">').insertAfter(evt.target);

            PropertyForm.teardownAll();
            PropertyForm.attachTo(root, {
                service: this.ontologyService,
                data: this.attr.data
            });
        };


        this.onPropertyChange = function (propertyChangeData) {
            if (propertyChangeData.id != this.attr.data.id) {
                return;
            }
            if(propertyChangeData.propertyName == 'title') {
                this.select('titleSelector').html(propertyChangeData.value);
            }
            this.select('propertiesSelector')
                .find('.property-' + propertyChangeData.propertyName + ' .value')
                .html(propertyChangeData.value);
        };

        this.displayProperties = function (properties){
            var self = this;

            this.ontologyService.properties()
                .done(function(ontologyProperties) {
                    self.ontologyProperties = ontologyProperties;
                    var filtered = filterPropertiesForDisplay(properties, ontologyProperties);

                    var iconProperty = _.findWhere(filtered, { key: '_glyphIcon' });

                    if (iconProperty) {
                        self.trigger(self.select('glyphIconSelector'), 'iconUpdated', { src: iconProperty.value });
                    }
                    var popoutEnabled = false;

                    if ($('#app').hasClass('fullscreen-details')) {
                        popoutEnabled = true;
                    }

                    var props = $(propertiesTemplate({properties:filtered, popout: popoutEnabled}));

                    require(['configuration/plugins/visibility/visibilityDisplay'], function(VisibilityDisplay) {
                        props.find('.visibility').each(function() {
                            VisibilityDisplay.attachTo(this, {
                                value: $(this).data('visibility')
                            })
                        });
                        self.$node.html(props);
                    });
                });
            self.trigger('toggleAuditDisplay', { displayed: false })
        };
    }

    function filterPropertiesForDisplay(properties, ontologyProperties) {
        var displayProperties = [];

        if ($.isArray(properties)) {
            var o = {};
            properties.forEach(function (p) {
                o[p.key] = p.value;
            });
            properties = o;
        }

        var keys = Object.keys(properties).sort(function(a,b) {
            if (a === 'startDate' && b === 'endDate') return -1;
            if (b === 'startDate' && a === 'endDate') return 1;

            return a < b ? -1 : a > b ? 1 : 0;
        });

        keys.forEach(function (name) {
            var displayName, value,
                ontologyProperty = ontologyProperties.byTitle[name],
                isRelationshipType = name === 'relationshipType' && properties._type;

            if (ontologyProperty) {
                displayName = ontologyProperty.displayName;

                if (ontologyProperty.dataType == 'date') {
                    value = formatters.date.dateString(parseInt(properties[name].value, 10));
                } else if (ontologyProperty.dataType === 'geoLocation') {
                    value = properties[name];
                } else {
                    value = properties[name].value;
                }

                var isRelationshipSourceProperty = name === 'source' && properties._type;
                if (/^[^_]/.test(name) && 
                    name !== 'boundingBox' &&
                    name !== 'title' &&
                    !isRelationshipSourceProperty) {
                    addProperty(name, displayName, value, properties[name]._visibility);
                }
            } else if (isRelationshipType) {
                addProperty(name, 'relationship type', properties[name].value);
            }
        });
        return displayProperties;

        function addProperty(name, displayName, value, visibility) {
            displayProperties.push({
                key: name,
                value: value,
                displayName: displayName || name,
                visibility: visibility
            });
        }
    }
});

