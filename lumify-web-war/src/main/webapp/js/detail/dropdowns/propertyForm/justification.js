
define([
    'flight/lib/component',
    'tpl!./justification',
    'tpl!./justificationRef',
    'data'
], function (
    defineComponent,
    template,
    templateRef,
    appData
) {
    'use strict';

    return defineComponent(Justification);


    function Justification() {

        this.defaultAttrs({
            fieldSelector: 'input'
        });

        this.after('initialize', function() {

            this.$node.html(
                template({})
            );

            this.on('valuepasted', this.onValuePasted);
        });

        this.onValuePasted = function(event, data) {
            var clipboard = appData.copiedDocumentText;

            if (clipboard && clipboard.text === data.value) {
                this.setValue(Object.freeze(clipboard));
            } else console.log(data);
        };

        this.setValue = function(value) {
            this.$node.html(templateRef(value));
            this.trigger('justificationchange', { value:value });
        };
    }
});
