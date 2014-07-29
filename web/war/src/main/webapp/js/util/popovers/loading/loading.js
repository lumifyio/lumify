
define([
    'flight/lib/component',
    '../withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(LoadingPopover, withPopover);

    function LoadingPopover() {

        this.defaultAttrs({
            buttonSelector: '.dialog-popover button'
        });

        this.before('initialize', function(node, config) {
            config.template = 'loading/template';
            config.message = config.message || i18n('popovers.loading.message');
            config.teardownOnTap = false;
        });

        this.after('initialize', function() {
            this.on(document, 'click', {
                buttonSelector: this.onCancel
            });
        });

        this.onCancel = function() {
            this.trigger('popovercancel');
        }
    }
});
