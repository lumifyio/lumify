

define(['util/withTeardown'], function(withTeardown) {
    'use strict';

    return function() {

        withTeardown.call(this);

        this.defaultAttrs({
            predicateSelector: 'select',
            visibleInputsSelector: 'input:visible',
            inputSelector: 'input,select',
            value: ''
        });

        this.after('teardown', function() {
            var inputs = this.select('visibleInputsSelector');
            inputs.tooltip('destroy');
            this.$node.empty();
        });

        this.after('initialize', function() {
            var inputs = this.select('visibleInputsSelector');

            this.$node.find('input').each(function() {
                $(this).attr('required', true)
            });

            if (this.attr.tooltip && this.$node.find('.input-prepend').length === 0) {
                inputs.eq(0)
                    .tooltip($.extend({ container:'body' }, this.attr.tooltip))
                    .data('tooltip').tip().addClass('field-tooltip');
            }

            if (this.attr.preventFocus !== true) {
                inputs.eq(0).focus();
            }
        });

        this.filterUpdated = function(values, predicate) {
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
                        predicate: predicate
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

        this.getValues = function() {
            return this.select('visibleInputsSelector').map(function() {
                return $(this).val();
            }).toArray();
        };

        this.updateRangeVisibility = function() {
            var v = this.select('predicateSelector').val();

            this.$node.find('.range-only').toggle(v === 'range');
        };

    };
});
