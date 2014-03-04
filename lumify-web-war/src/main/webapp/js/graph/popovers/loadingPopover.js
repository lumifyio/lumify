
define([
    'flight/lib/component',
    './withVertexPopover'
], function(
    defineComponent,
    withVertexPopover) {
    'use strict';

    return defineComponent(LoadingPopover, withVertexPopover);

    function LoadingPopover() {

        this.defaultAttrs({
            buttonSelector: 'button'
        });

        this.after('teardown', function() {
        });

        this.before('initialize', function(node, config) {
            config.template = 'loadingPopover';
            config.message = config.message || 'Loading...';
            config.teardownOnTap = false;
        });

        this.after('initialize', function() {
            this.on('click', {
                buttonSelector: this.onCancel
            });
        });

        this.onCancel = function() {
            this.trigger('popovercancel');
        }
    }
});
