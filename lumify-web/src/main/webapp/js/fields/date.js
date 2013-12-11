
define([
    'flight/lib/component',
    'tpl!./date',
    './withPropertyField'
], function(defineComponent, template, withPropertyField) {
    'use strict';

    return defineComponent(DateField, withPropertyField);

    function formatDate(date) {
        if (Object.prototype.toString.call(date) !== "[object Date]" || isNaN(date.getTime())) return '';
                
        return date.getFullYear() + '-' + pad(date.getMonth() + 1) + '-' + pad(date.getDate());

        function pad(num) {
            var str = '' + num;
            while (str.length !== 2) {
                str = '0' + str;
            }
            return str;
        }
    }

    function DateField() {

        this.after('initialize', function() {
            var value = '';

            if (this.attr.value) {
                var millis = _.isString(this.attr.value) ? Number(this.attr.value) : this.attr.value,
                    date = new Date(millis);
                value = formatDate(date);
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

