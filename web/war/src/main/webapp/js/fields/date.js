
define([
    'flight/lib/component',
    'hbs!./dateTpl',
    'hbs!./dateTimezone',
    './withPropertyField',
    './withHistogram',
    'util/formatters',
    'util/popovers/withElementScrollingPositionUpdates',
    'chrono',
    'jstz'
], function(
    defineComponent,
    template,
    timezoneTemplate,
    withPropertyField,
    withHistogram,
    F,
    withPositionUpdates,
    chrono,
    jstz) {
    'use strict';

    return defineComponent(DateField, withPropertyField, withHistogram, withPositionUpdates);

    function DateField() {

        this.defaultAttrs({
            timeFieldSelector: '.timepicker',
            timezoneSelector: '.timezone'
        });

        this.before('initialize', function(node, config) {
            if (_.isUndefined(config.preventFocus)) {
                config.preventFocus = true;
            }
        });

        this.after('initialize', function() {
            var self = this,
                value = '',
                dateString = '',
                timeString = '';

            this.displayTime = this.attr.property.displayType !== 'dateOnly';

            if (this.attr.value) {
                dateString = value = F.date.dateStringUtc(this.attr.value);
                timeString = F.date.timeString(this.attr.value);
            }

            this.$node.html(template({
                dateString: dateString,
                timeString: timeString,
                today: F.date.dateString(new Date()),
                todayTime: F.date.timeString(new Date()),
                displayTime: this.displayTime,
                predicates: this.attr.predicates
            }));

            this.updateRangeVisibility();
            this.updateTimezone()

            this.getValues = function() {
                var inputs = this.$node.hasClass('alternate') ?
                        this.$node.find('.input-row input') :
                        this.select('visibleInputsSelector'),
                    values = inputs.map(function() {
                        return $(this).val();
                    }).toArray();

                if (this.displayTime && values.length > 1) {
                    var newValues = [], i;
                    for (i = 0; i < values.length; i += 2) {
                        newValues.push(values[i] + ' ' + values[i + 1]);
                    }
                    values = newValues;
                }

                return values.map(function(v) {
                    if (self.displayTime) {
                        return F.timezone.dateTimeStringToUtc(v, self.currentTimezone.name);
                    }
                    return v;
                });
            };

            this.select('timeFieldSelector').timepicker({
                template: false,
                showInputs: false,
                minuteStep: 15,
                defaultTime: timeString || false,
                showMeridian: false
            });

            this.on('change keyup', {
                    inputSelector: function() {
                        this.updateRangeVisibility();
                        this.updateTimezone();
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
            });

            this.timezoneOpened = false;
            this.on('click', {
                timezoneSelector: this.onTimezoneOpen
            });

            this.on('selectTimezone', this.onSelectTimezone);
            this.updateTimezone();
        });

        this.onSelectTimezone = function(event, data) {
            if (data.name) {
                this.updateTimezone(data);
            }
        };

        this.updateTimezone = function(tz) {
            if (this.displayTime) {
                var self = this,
                    values = this.getValues(),
                    date = (values && values[0]) ? new Date(values[0]) : null,
                    shiftTime = tz && tz.shiftTime;

                if (tz) {
                    if (!_.isString(tz)) {
                        tz = tz.name;
                    }
                    if (shiftTime) {
                        var inputs = this.$node.find('input');

                        if (values && values[0] && inputs.length > 1) {
                            date = F.timezone.date(values[0], 'Etc/UTC');
                            inputs.eq(0).val(date.toString('yyyy-MM-dd', tz)).datepicker('update')
                            inputs.eq(1).data('timepicker').setTime(date.toString('HH:mm', tz));
                        } else if (values && values[1] && inputs.length > 3) {
                            date = F.timezone.date(values[1], 'Etc/UTC');
                            inputs.eq(2).val(date.toString('yyyy-MM-dd', tz)).datepicker('update');
                            inputs.eq(3).data('timepicker').setTime(date.toString('HH:mm', tz));
                        }
                    }
                    this.currentTimezone = F.timezone.lookupTimezone(tz, date.getTime());
                } else {
                    if (!this.currentTimezone) {
                        this.currentTimezone = F.timezone.currentTimezone(date);
                    } else {
                        this.currentTimezone = F.timezone.lookupTimezone(this.currentTimezone.name, date);
                    }
                }

                this.currentTimezoneMetadata = {
                    'http://lumify.io#sourceTimezone': this.currentTimezone.name,
                    'http://lumify.io#sourceTimezoneOffset': this.currentTimezone.offset,
                    'http://lumify.io#sourceTimezoneOffsetDst': this.currentTimezone.tzOffset
                };

                this.select('timezoneSelector').replaceWith(
                    timezoneTemplate(this.currentTimezone)
                );

            }

            this.filterUpdated(
                this.getValues(),
                this.select('predicateSelector').val(),
                {
                    metadata: this.currentTimezoneMetadata
                }
            );

        };

        this.onTimezoneOpen = function(event) {
            var self = this,
                $target = $(event.target).closest('.timezone');

            event.preventDefault();

            if (!this.Timezone) {
                require(['util/popovers/timezone/timezone'], function(Timezone) {
                    self.Timezone = Timezone;
                    self.onTimezoneOpen(event);
                });
                return;
            }

            if ($target.lookupComponent(this.Timezone)) {
                return;
            }

            this.Timezone.attachTo($target, {
                scrollSelector: '.content',
                timezone: this.currentTimezone.name,
                sourceTimezone: this.attr.vertexProperty &&
                    this.attr.vertexProperty['http://lumify.io#sourceTimezone']
            });
        };

        this.isValid = function() {
            var self = this,
                dateRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*$/,
                dateTimeRegex = /^\s*\d{4}-\d{1,2}-\d{1,2}\s*\d{1,2}:\d{1,2}\s*$/;

            return _.every(this.getValues(), function(v, i) {
                if (self.displayTime) {
                    return dateTimeRegex.test(v);
                } else {
                    return dateRegex.test(v);
                }
            });
        };
    }
});
