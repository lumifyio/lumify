
define([
    'flight/lib/component',
    'service/ontology',
    'service/vertex',
    'service/relationship',
    './dropdowns/propertyForm/propForm',
    'tpl!./properties',
    'sf'
], function (defineComponent, OntologyService, VertexService, RelationshipService, PropertyForm, propertiesTemplate, sf) {
    'use strict';

    var component = defineComponent(Properties);
    component.filterPropertiesForDisplay = filterPropertiesForDisplay;
    return component;

    function Properties() {

        this.ontologyService = new OntologyService();
        this.vertexService = new VertexService();
        this.relationshipService = new RelationshipService();

        this.defaultAttrs({
            addNewPropertiesSelector: '.add-new-properties'
        });

        this.after('initialize', function () {
            this.on('click', {
                addNewPropertiesSelector: this.onAddNewPropertiesClicked
            });
            this.on('addProperty', this.onAddProperty);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.$node.html(propertiesTemplate({properties:null}));
            this.displayProperties(this.attr.data.properties);
        });

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

