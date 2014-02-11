
define([
    'flight/lib/component',
    'tpl!./form'
], function(
    defineComponent,
    formTemplate) {
    'use strict';

    return defineComponent(Visibility);

    function Visibility() {

        this.defaultAttrs({
            fieldSelector: 'input'
        })

        this.after('initialize', function() {
            this.$node.html(formTemplate({
                value: this.attr.value || ''
            }));

            this.onChange = _.debounce(this.onChange.bind(this), 250);

            this.on('visibilityclear', this.onClear);
            this.on('change keyup paste', {
                fieldSelector: this.onChange
            })
        });

        this.onClear = function(event, data) {
            this.select('fieldSelector').val('');
        };

        this.onChange = function(event, data) {
            var value = this.select('fieldSelector').val(); 
            this.trigger('visibilitychange', {
                value: value, 
                valid: value.length > 0
            });
        };
    }
});
