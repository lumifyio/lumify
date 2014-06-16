
define([
    'flight/lib/component',
    'hbs!./dateTpl',
    './withPropertyField',
    './withHistogram',
    'util/formatters',
    'chrono'
], function(defineComponent, template, withPropertyField, withHistogram, F, chrono) {
    'use strict';

    return defineComponent(DateField, withPropertyField, withHistogram);

    function DateField() {

        this.defaultAttrs({
            timeFieldSelector: '.timepicker'
        });

        this.before('initialize', function(node, config) {
            if (_.isUndefined(config.preventFocus)) {
                config.preventFocus = true;
            }
        });

        this.after('initialize', function() {
            var value = '',
                dateString = '',
                timeString = '';

            if (this.attr.value) {
                dateString = value = F.date.dateString(this.attr.value);
                timeString = F.date.timeString(this.attr.value);
            }

            this.getValues = function() {
                var values = this.select('visibleInputsSelector').map(function() {
                        return $(this).val();
                    }).toArray();

                if (this.attr.property.displayTime && values.length > 1) {
                    var newValues = [], i;
                    for (i = 0; i < values.length; i += 2) {
                        newValues.push(values[i] + ' ' + values[i + 1]);
                    }
                    return newValues;
                }

                return values;
            };

            this.$node.html(template({
                dateString: dateString,
                timeString: timeString,
                today: F.date.dateString(new Date()),
                todayTime: F.date.timeString(new Date()),
                displayTime: this.attr.property.displayTime,
                predicates: this.attr.predicates
            }));

            this.select('timeFieldSelector').timepicker({
                template: false,
                showInputs: false,
                minuteStep: 15,
                defaultTime: timeString || false,
                showMeridian: false
            });

            this.updateRangeVisibility();
            this.filterUpdated(
                this.getValues(),
                this.select('predicateSelector').val()
            );

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
                            self.val(F.date.dateString(date));
                            self.datepicker('setDate', date);
                            self.next('input.timepicker').timepicker('setTime', date);
                            self.datepicker('update');
                            self.blur();
                        }
                    }
                }, 500)
            })
        });

        this.isValid = function() {
            var dateRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*$/,
                dateTimeRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*\d{1,2}:\d{1,2}\s*$/,
                displayTime = this.attr.property.displayTime;

            return _.every(this.getValues(), function(v, i) {
                if (displayTime) {
                    return dateTimeRegex.test(v);
                } else {
                    return dateRegex.test(v);
                }
            });
        };
    }
});
