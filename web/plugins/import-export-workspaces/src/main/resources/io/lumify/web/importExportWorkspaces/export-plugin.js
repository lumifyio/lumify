require([
    'configuration/admin/plugin',
    'hbs!x-tpl',
    'util/formatters',
    'util/messages',
    'service/workspace',
    'd3'
], function(
    defineLumifyAdminPlugin,
    template,
    F,
    i18n,
    WorkspaceService,
    d3
    ) {
    'use strict';

    var workspaceService = new WorkspaceService();

    return defineLumifyAdminPlugin(WorkspaceExport, {
        section: i18n('admin.workspace.section'),
        name: i18n('admin.workspace.button.export'),
        subtitle: i18n('admin.workspace.export.subtitle')
    });

    function WorkspaceExport() {

        this.defaultAttrs({
            selectSelector: 'select'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({}));

            this.on('change', {
                selectSelector: this.onChange
            });

            workspaceService.list()
                .always(function() {
                    self.$node.find('.badge').remove();
                })
                .fail(this.showError.bind(this, i18n('admin.workspace.export.workspace.error')))
                .done(function(result) {
                    self.select('selectSelector')
                        .append(
                            result.workspaces.map(function(workspace) {
                                return $('<option>')
                                    .val(workspace.workspaceId)
                                    .text(workspace.title);
                            })
                        ).change();

                    if (result.workspaces.length) {
                        self.$node.find('a').removeAttr('disabled');
                    }
                })
        });

        this.onChange = function() {
            var select = this.$node.find('select'),
                workspaceId = select.val();

            this.$node.find('a').attr({
                download: $(select.get(0).selectedOptions).text() + '.lumifyWorkspace',
                href: 'admin/workspace/export?' + $.param({ workspaceId: workspaceId })
            });
        };

    }
});
