define([
    'require',
    'flight/lib/component',
    'util/withDataRequest'
], function(require, defineComponent, withDataRequest) {
    'use strict';

    return defineComponent(CompoundField, withDataRequest);

    function CompoundField() {

        var values = {};

        this.after('initialize', function() {
            var self = this;

            this.on('propertychange', this.onDependentPropertyChange);
            this.on('propertyinvalid', this.onDependentPropertyInvalid);

            this.dataRequest('ontology', 'properties')
                .done(function(ontologyProperties) {
                    self.ontologyProperties = ontologyProperties;
                    self.render();
                });
        });

        this.onDependentPropertyChange = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();

            values[data.propertyId] = data.values;

            if (this.isValid()) {
                this.trigger('propertychange', {
                    id: this.attr.id,
                    propertyId: this.attr.property.title,
                    values: this.getValues()
                });
            } else {
                this.trigger('propertyinvalid', {
                    id: this.attr.id,
                    propertyId: this.attr.property.title
                });
            }
        };

        this.onDependentPropertyInvalid = function(event, data) {
            if ($(event.target).is(this.$node)) {
                return;
            }

            event.stopPropagation();
            this.trigger('propertyinvalid', {
                id: this.attr.id,
                propertyId: this.attr.property.title
            });
        };

        this.getValues = function() {
            return _.chain(this.attr.property.dependentPropertyIris)
                .map(function(iri) {
                    return values[iri];
                })
                .value()
        };

        this.isValid = function() {
            var values = this.getValues();

            return _.every(values, function(v) {
                return v && v.length && v[0].length
            });
        };

        this.render = function() {
            var self = this,
                fields = $();

            this.attr.property.dependentPropertyIris.forEach(function(propertyIri, i) {
                var property = self.ontologyProperties.byTitle[propertyIri],
                    fieldContainer = $('<div>').addClass('compound-field');

                require([
                    property.possibleValues ?
                        '../restrictValues' :
                        '../' + property.dataType
                ], function(PropertyField) {
                    PropertyField.attachTo(fieldContainer, {
                        property: property,
                        vertexProperty: null, //vertexProperty,
                        value: '', //previousValue,
                        predicates: self.attr.predicates,
                        focus: i === 0
                    });
                })
                fields = fields.add(fieldContainer);
            })

            this.$node.empty().append(fields);
        };
    }
});
