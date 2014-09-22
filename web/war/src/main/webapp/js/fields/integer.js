
define([
    'flight/lib/component',
    'tpl!./double',
    './withPropertyField',
    './withHistogram'
], function(defineComponent, template, withPropertyField, withHistogram) {
    'use strict';

    return defineComponent(IntegerField, withPropertyField, withHistogram);

    function makeNumber(v) {
        return parseInt(v, 10);
    }

    function IntegerField() {

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
