
define([
    'flight/lib/component',
    'tpl!./form'
], function(
    defineComponent,
    formTemplate) {
    'use strict';

    return defineComponent(VisibilityEditor);

    function VisibilityEditor() {

        this.defaultAttrs({
            fieldSelector: 'input'
        })

        this.after('initialize', function() {
            this.$node.html(formTemplate({
                value: $.trim(_.isUndefined(this.attr.value) ? '' : this.attr.value)
            }));

            this.onChange = _.debounce(this.onChange.bind(this), 250);

            this.on('visibilityclear', this.onClear);
            this.on('change keyup paste', {
                fieldSelector: this.onChange
            });

            this.onChange();
        });

        this.onClear = function(event, data) {
            this.select('fieldSelector').val('');
        };

        this.onChange = function(event, data) {
            var value = $.trim(this.select('fieldSelector').val());
            this.trigger('visibilitychange', {
                value: value, 
                valid: true
            });
        };
    }
});
