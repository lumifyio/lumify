
define([
    'flight/lib/component',
    './withVertexPopover'
], function(
    defineComponent,
    withVertexPopover) {
    'use strict';

    return defineComponent(ControlDragPopover, withVertexPopover);

    function ControlDragPopover() {

        this.defaultAttrs({
            buttonSelector: 'button'
        });

        this.before('initialize', function(node, config) {
            config.template = 'controlDragPopover';
        });

        this.after('initialize', function() {
            this.on('click', {
                buttonSelector: this.onButton
            });
        });

        this.onButton = function(event) {
            var self = this,
                component = $(event.target).data('component');

            require(['graph/popovers/' + component], function(ComponentPopover) {

                ComponentPopover.teardownAll();
                ComponentPopover.attachTo(self.node, self.attr);
                self.teardown();
            });
        }
    }

});
