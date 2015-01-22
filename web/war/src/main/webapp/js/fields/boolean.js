
define([
    'flight/lib/component',
    'hbs!./booleanTpl',
    './withPropertyField',
    'util/formatters'
], function(defineComponent, template, withPropertyField, F) {
    'use strict';

    return defineComponent(BooleanField, withPropertyField);

    function makeNumber(v) {
        return parseFloat(v, 10);
    }

    function BooleanField() {

        this.defaultAttrs({
            booleanSelector: '.input-prepend'
        });

        this.after('initialize', function() {
            this.$node.html(template(this.attr));

            this.on('click', {
                booleanSelector: this.onClick
            });

            this.on('change', {
                inputSelector: this.onToggle
            });

            this.update();
        });

        this.onToggle = function(event) {
            this.update();
        };

        this.onClick = function(event) {
            if ($(event.target).is('input')) {
                return;
            }

            var input = this.select('inputSelector'),
                val = !input.prop('checked');

            input.prop('checked', val);

            this.update();
        };

        this.update = function() {
            var input = this.select('inputSelector'),
                val = input.prop('checked');

            this.$node.find('.input-row .display').text(
                i18n(true, 'field.boolean.' + val + '.' + this.attr.property.title) ||
                F.boolean.pretty(val)
            );

            this.filterUpdated(
                this.getValues().map(function(v) {
                    return v ? 'true' : 'false';
                })
            );
        }

        this.isValid = function() {
            return true;
        };
    }
});
