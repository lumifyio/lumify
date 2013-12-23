
define([
    'flight/lib/component',
    'tpl!./date',
    './withPropertyField',
    'util/formatters'
], function(defineComponent, template, withPropertyField, formatters) {
    'use strict';

    return defineComponent(DateField, withPropertyField);

    function DateField() {

        this.after('initialize', function() {
            var value = '';

            if (this.attr.value) {
                value = formatters.date.dateString(this.attr.value);
            }

            this.$node.html(template({
                value: value,
                predicates: this.attr.predicates
            }));

            this.updateRangeVisibility();

            this.on('change keyup', {
                inputSelector: function() {

                    this.updateRangeVisibility();

                    this.filterUpdated(
                        this.getValues(),
                        this.select('predicateSelector').val()
                    );
                }
            });
        });

        this.isValid = function() {
            return _.every(this.getValues(), function(v) {
                return (/^\s*\d{4}-\d{1,2}-\d{1,2}\s*$/).test(v);
            });
        };
    }
});

