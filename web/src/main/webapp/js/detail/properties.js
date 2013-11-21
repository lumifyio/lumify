
define([
    'flight/lib/component',
    'service/ontology',
    'service/vertex',
    'service/relationship',
    'service/audit',
    'util/formatters',
    './dropdowns/propertyForm/propForm',
    'tpl!./properties',
    'tpl!./audit-list',
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
    auditsListTemplate,
    sf) {
    'use strict';

    var component = defineComponent(Properties);
    component.filterPropertiesForDisplay = filterPropertiesForDisplay;
    return component;

    function Properties() {

        this.ontologyService = new OntologyService();
        this.vertexService = new VertexService();
        this.relationshipService = new RelationshipService();
        this.auditService = new AuditService();

        this.defaultAttrs({
            addNewPropertiesSelector: '.add-new-properties',
            entityAuditsSelector: '.entity_audit_events'
        });

        this.after('initialize', function () {
            this.on('click', {
                addNewPropertiesSelector: this.onAddNewPropertiesClicked
            });
            this.on('addProperty', this.onAddProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node
                .closest('.type-content')
                .off('.properties')
                .on('toggleAuditDisplay.properties', this.onToggleAuditing.bind(this));

            this.$node.html(propertiesTemplate({properties:null}));
            this.displayProperties(this.attr.data.properties);
        });

        this.onToggleAuditing = function(event, data) {
            var self = this,
                auditsEl = this.select('entityAuditsSelector');

            if (data.displayed) {
                auditsEl.html('<div class="nav-header">Audits<span class="badge loading"/></div>').show();
                this.$node.find('.audit-list').remove();

                this.auditService.getAudits(this.attr.data.id)
                    .done(function(auditResponse) {
                        var audits = auditResponse.auditHistory.reverse(),
                            propertyAudits = _.filter(audits, function(a) { return /^Set/i.test(a.message); });

                        self.select('entityAuditsSelector').html(auditsListTemplate({
                            audits: audits,
                            formatters: formatters,
                            hideRegex: /^(BEGIN|END|Set)/i
                        }));

                        self.updatePropertyAudits(propertyAudits);
                        auditsEl.show();                        
                    });
            } else {
                auditsEl.hide();
                this.$node.find('.audit-list').remove();
            }
        };

        this.updatePropertyAudits = function(audits) {
            var self = this,
                extractRegex = /^Set\s+([^\s]+)\s+from\s+([^\s]+)\s+to\s+(.+)$/,
                auditsByProperty = {};

            audits.forEach(function(audit) {
                var match = audit.message.match(extractRegex);
                if (match && match.length === 4) {
                    match.shift();
                    var propertyName = match.shift(),
                        fromValue = match.shift(),
                        toValue = match.shift();

                    if (!auditsByProperty[propertyName]) {
                        auditsByProperty[propertyName] = [];
                    }

                    audit.fromValue = fromValue;
                    audit.toValue = toValue;
                    if (!$('#app').hasClass('fullscreen-details')) {
                        var geoMatch = audit.toValue.match(/^point\(([\d.-]+)\s*,\s*([\d.-]+)\s*\)$/);
                        if (geoMatch && geoMatch.length === 3) {
                            audit.geoLocation = {
                                latitude: geoMatch[1],
                                longitude: geoMatch[2]
                            };
                        }
                    }
                    if (/^\d+-\d+-\d+$/.test(audit.toValue)) {
                        audit.toValue = sf("{0:yyyy/MM/dd}", new Date(audit.toValue + " 00:00"));
                    }

                    auditsByProperty[propertyName].push(audit);
                        
                } else console.warn('Unable to extract property audit details: ', audit.message);
            });

            Object.keys(auditsByProperty).forEach(function(propertyName) {
                var propLi = self.$node.find('.property-' + propertyName);
                propLi.append(auditsListTemplate({
                    audits: auditsByProperty[propertyName],
                    formatters: formatters,
                    hideRegex: null
                }));
            });
        };

        this.onVerticesUpdated = function(event, data) {
            var self = this;

            data.vertices.forEach(function(vertex) {
                if (vertex.id === self.attr.data.id) {
                    self.displayProperties(vertex.properties);
                }
            });
        };

        this.onAddProperty = function (event, data) {
            var self = this;

            if (self.attr.data.properties._type === 'relationship') {
                self.relationshipService.setProperty(
                        data.property.name,
                        data.property.value,
                        this.attr.data.properties.source,
                        this.attr.data.properties.target,
                        this.attr.data.properties.relationshipLabel
                ).done(function(newProperties) {
                    var properties = $.extend({}, self.attr.data.properties, newProperties);
                    self.displayProperties(properties);
                    self.trigger('updateRelationships', [{
                        id: self.attr.data.id,
                        properties: properties
                    }]);
                }).fail(onFail);
            } else {
                self.vertexService.setProperty(
                    this.attr.data.id,
                    data.property.name,
                    data.property.value)
                    .fail(onFail)
                    .done(function(vertexData) {
                        self.displayProperties(vertexData.properties);
                        self.trigger (document, "updateVertices", { vertices: [vertexData.vertex] });
                    });
            }

            function onFail(err) {
                if (err.status == 400) {
                    console.error('Validation error');
                    return self.trigger(self.$node.find('.underneath'), 'addPropertyError', {});
                }
                return 0;
            }
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

            this.ontologyService.properties().done(function(ontologyProperties) {
                var filtered = filterPropertiesForDisplay(properties, ontologyProperties);

                var iconProperty = _.findWhere(filtered, { key: '_glyphIcon' });

                if (iconProperty) {
                    self.trigger(self.select('glyphIconSelector'), 'iconUpdated', { src: iconProperty.value });
                }
                var popoutEnabled = false;

                if ($('#app').hasClass('fullscreen-details')) {
                    popoutEnabled = true;
                }

                var props = propertiesTemplate({properties:filtered, popout: popoutEnabled});
                self.$node.html(props);
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
                isRelationshipType = name === 'relationshipType' && properties._type === 'relationship';

            if (ontologyProperty) {
                displayName = ontologyProperty.displayName;

                if (ontologyProperty.dataType == 'date') {
                    value = sf("{0:yyyy/MM/dd}", new Date(parseInt(properties[name], 10)));
                } else {
                    value = properties[name];
                }

                var isRelationshipSourceProperty = name === 'source' && properties._type === 'relationship';
                if (/^[^_]/.test(name) && 
                    name !== 'boundingBox' &&
                    !isRelationshipSourceProperty) {
                    addProperty(name, displayName, value);
                }
            } else if (isRelationshipType) {
                addProperty(name, 'relationship type', properties[name]);
            }
        });
        return displayProperties;

        function addProperty(name, displayName, value) {
            displayProperties.push({
                key: name,
                value: value,
                displayName: displayName || name
            });
        }
    }
});

