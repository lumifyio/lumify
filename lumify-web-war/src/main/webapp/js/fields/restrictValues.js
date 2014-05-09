define([
    'flight/lib/component',
    './withPropertyField',
    'hbs!./restrictValuesTpl'
], function(defineComponent, withPropertyField, template) {
    'use strict';

    return defineComponent(RestrictValuesField, withPropertyField);

    function RestrictValuesField() {

        this.defaultAttrs({
            selectSelector: 'select'
        });

        this.after('initialize', function() {
            var val = this.attr.value;

            this.filterUpdated(val);

            this.$node.html(template({
                predicates: this.attr.predicates,
                displayName: this.attr.property.displayName,
                values: _.map(this.attr.property.possibleValues, function(possibleValue) {
                    if (possibleValue.key === val) {
                        possibleValue.selected = true;
                    }

                    return possibleValue;
                })
            }));

            this.on('change', {
                selectSelector: function(event) {
                    var val = this.select('selectSelector').val()

                    this.filterUpdated(val);
                }
            })
        });

        this.isValid = function() {
            var vals = this.getValues();

            return vals.length && _.every(vals, function(v) {
                return $.trim(v).length > 0;
            });
        };

    }
});
