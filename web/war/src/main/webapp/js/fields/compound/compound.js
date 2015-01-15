define([
    'require',
    'flight/lib/component',
    'util/withDataRequest'
], function(require, defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(CompoundField, withDataRequest);

    function CompoundField() {

        this.after('initialize', function() {
            var self = this;

            this.on('propertychange', this.onDependentPropertyChange);

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    self.ontologyProperties = ontologyProperties;
                    self.render();
                });
        });

        this.onDependentPropertyChange = function(event) {
            event.stopPropagation();
        };

        this.render = function() {
            var self = this,
                fields = $();

            this.attr.property.dependentPropertyIris.forEach(function(propertyIri) {
                var property = self.ontologyProperties.byTitle[propertyIri],
                    fieldContainer = $('<div>')
                        .text(property.displayName)
                        .addClass('compound-field');

                require([
                    property.possibleValues ?
                        '../restrictValues' :
                        '../' + property.dataType
                ], function(PropertyField) {
                    PropertyField.attachTo(fieldContainer, {
                        property: property,
                        vertexProperty: null, //vertexProperty,
                        value: '', //previousValue,
                        predicates: self.attr.predicates
                    });
                })
                fields = fields.add(fieldContainer);
            })

            this.$node.append(fields);
        };
    }
});
