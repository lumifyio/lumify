
define([
    'flight/lib/component',
    'tpl!./date',
    './withPropertyField',
    'util/formatters',
    'chrono'
], function(defineComponent, template, withPropertyField, formatters, chrono) {
    'use strict';

    return defineComponent(DateField, withPropertyField);

    function DateField() {

        this.before('initialize', function(node, config) {
            if (_.isUndefined(config.preventFocus)) {
                config.preventFocus = true;
            }
        });

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

            this.$node.find('input').on('paste', function(event) {
                var self = $(this);

                self.datepicker('hide');
                self[0].select();
                
                _.delay(function() {
                    var pasted = self.val();

                    if (pasted) {
                        var date = chrono.parseDate(pasted) 
                        if (date) {
                            self.val(formatters.date.dateString(date));
                            self.datepicker('setDate', date)
                            self.datepicker('update');
                            self.blur();
                        }
                    }
                }, 500)
            })
        });

        this.isValid = function() {
            return _.every(this.getValues(), function(v) {
                return (/^\s*\d{4}-\d{1,2}-\d{1,2}\s*$/).test(v);
            });
        };
    }
});

