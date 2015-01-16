
define(['util/withTeardown'], function(withTeardown) {
    'use strict';

    return function() {

        withTeardown.call(this);

        this.defaultAttrs({
            predicateSelector: 'select.predicate',
            visibleInputsSelector: 'input:visible,select:not(.predicate):visible',
            inputSelector: 'input,textarea,select',
            value: ''
        });

        this.after('teardown', function() {
            var inputs = this.select('visibleInputsSelector');
            inputs.tooltip('destroy');
            this.$node.empty();
        });

        this.after('initialize', function() {
            var inputs = this.select('visibleInputsSelector'),
                inputsNoSelects = inputs.not('select');

            this.$node.find('input:not([type=checkbox])').each(function() {
                var $this = $(this);
                if ($this.data('optional') !== true) {
                    $this.attr('required', true)
                }
            });

            if (inputsNoSelects.length && this.attr.tooltip &&
                this.attr.disableTooltip !== true &&
                this.$node.find('.input-prepend').length === 0) {

                inputsNoSelects.eq(0)
                    .tooltip($.extend({ container: 'body' }, this.attr.tooltip))
                    .data('tooltip').tip().addClass('field-tooltip');
            }

            if (this.attr.focus !== false) {
                inputs.eq(0).focus();
            }
        });

        this.filterUpdated = function(values, predicate, options) {
            values = $.isArray(values) ? values : [values];

            if (!_.isFunction(this.isValid) || this.isValid()) {

                if (
                    (!this._previousValues ||
                        (this._previousValues && !_.isEqual(this._previousValues, values))) ||

                    (!this._previousPredicate ||
                         (this._previousPredicate && !_.isEqual(this._previousPredicate, predicate)))
                ) {

                    this.trigger('propertychange', {
                        id: this.attr.id,
                        propertyId: this.attr.property.title,
                        values: values,
                        predicate: predicate,
                        metadata: options && options.metadata,
                        options: options
                    });
                }

                this._previousValues = values;
                this._previousPredicate = predicate;
                this._markedInvalid = false;
            } else if (!this._markedInvalid) {
                this._markedInvalid = true;
                this.trigger('propertyinvalid', {
                    id: this.attr.id,
                    propertyId: this.attr.property.title
                });
            }
        };

        this.setValues = function(val1, val2, options) {
            var inputs = this.$node.find('.input-row input'),
                values = ['', ''];

            if (val1 && _.isDate(val1)) {
                if (inputs.length === 4) {
                    inputs.eq(0).datepicker('setDate', val1);
                    inputs.eq(1).timepicker('setTime', val1);
                    inputs.eq(2).datepicker('setDate', val2);
                    inputs.eq(3).timepicker('setTime', val2);
                } else {
                    inputs.eq(0).datepicker('setDate', val1);
                    inputs.eq(1).datepicker('setDate', val2);
                }
                values = this.getValues();
            } else {
                if (val1) {
                    values = [val1.toFixed(2), val2.toFixed(2)];
                }
                inputs.eq(0).val(values[0]);
                inputs.eq(1).val(values[1]);
            }

            this.filterUpdated(
                values,
                this.select('predicateSelector').val(),
                options
            );
        };

        this.getValues = function() {
            var inputs = this.$node.hasClass('alternate') ?
                this.$node.find('.input-row input') :
                this.select('visibleInputsSelector');

            return inputs.map(function() {
                var $this = $(this);
                if ($this.is('input[type=checkbox]')) {
                    return $this.prop('checked');
                }
                return $(this).val();
            }).toArray();
        };

        this.updateRangeVisibility = function() {
            var v = this.select('predicateSelector').val();

            this.$node.find('.range-only').toggle(v === 'range');
        };

    };
});
