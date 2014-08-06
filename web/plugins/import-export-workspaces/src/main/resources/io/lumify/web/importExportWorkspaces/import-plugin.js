
require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/importExportWorkspaces/import',
    'configuration/admin/utils/fileUpload',
    'util/messages',
], function(
    defineLumifyAdminPlugin,
    template,
    FileUpload,
    i18n
) {
    'use strict';

    defineLumifyAdminPlugin(WorkspaceImport, {
        section: i18n('admin.workspace.section'),
        name: i18n('admin.workspace.button.import'),
        subtitle: i18n('admin.workspace.import.subtitle')
    });

    function WorkspaceImport() {

        this.defaultAttrs({
            uploadSelector: '.upload',
            importButtonSelector: 'button.import'
        });

        this.after('initialize', function() {
            this.on('fileChanged', this.onFileChanged)
            this.on('click', {
                importButtonSelector: this.onImport
            });
            this.$node.html(template({}));

            FileUpload.attachTo(this.select('uploadSelector'));
        });

        this.onImport = function() {
            var self = this,
                importButton = this.select('importButtonSelector').attr('disabled', true);

            this.handleSubmitButton(importButton,
                this.adminService.workspaceImport(this.workspaceFile)
                    .fail(this.showError.bind(this, i18n('admin.workspace.import.error')))
                    .done(this.showSuccess.bind(this, i18n('admin.workspace.import.success')))
                    .done(function() {
                        self.trigger(self.select('uploadSelector'), 'reset');
                    })
            );
        };

        this.onFileChanged = function(event, data) {
            this.workspaceFile = data.file;
            if (data.file) {
                this.select('importButtonSelector').removeAttr('disabled');
            } else {
                this.select('importButtonSelector').attr('disabled', true);
            }
        }
    }
});