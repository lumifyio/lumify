
define([
    'flight/lib/component',
    '../withPopover'
], function(
    defineComponent,
    withPopover) {
    'use strict';

    return defineComponent(ExportWorkspace, withPopover);

    function ExportWorkspace() {

        this.defaultAttrs({
            cancelButtonSelector: 'button.btn-default'
        });

        this.before('initialize', function(node, config) {
            config.template = 'exportWorkspace/template';
            config.title = i18n('popovers.export_workspace.title', config.exporter.menuItem);

            this.after('setupWithTemplate', function() {
                var self = this,
                    node = this.popover.find('.plugin-content'),
                    workspaceId = this.attr.workspaceId,
                    exporter = this.attr.exporter;

                this.on(this.popover, 'click', {
                    cancelButtonSelector: this.onCancel
                });

                require([this.attr.exporter.componentPath], function(C) {
                    C.attachTo(node, {
                        workspaceId: workspaceId,
                        exporter: exporter
                    });

                    self.positionDialog();
                });
            });

            this.onCancel = function() {
                this.teardown();
            }
        });
    }
});
