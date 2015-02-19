
define([
    'flight/lib/component',
    'tpl!./currency',
    './withHistogram',
    './withPropertyField'
], function(defineComponent, template, withHistogram, withPropertyField) {
    'use strict';

    return defineComponent(CurrencyField, withPropertyField, withHistogram);

    function makeNumber(v) {
        return parseFloat(v.replace(/[^0-9.]/g, ''), 10);
    }

    function CurrencyField() {

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.updateRangeVisibility();

            this.on('change keyup', {
                inputSelector: function() {

                    this.updateRangeVisibility();

                    this.filterUpdated(
                        this.getValues().map(function(v) {
                            return makeNumber(v);
                        }),
                        this.select('predicateSelector').val()
                    );
                }
            });
        });

        this.isValid = function() {
            var values = this.getValues();

            return _.every(values, function(v) {
                return v.length && _.isNumber(makeNumber(v));
            });
        };
    }
});
