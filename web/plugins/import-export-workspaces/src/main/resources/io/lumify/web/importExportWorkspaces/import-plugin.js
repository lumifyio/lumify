
require([
    'configuration/admin/plugin',
    'hbs!io/lumify/web/importExportWorkspaces/import',
    'configuration/admin/utils/fileUpload',
    'util/messages',
    'util/withDataRequest'
], function(
    defineLumifyAdminPlugin,
    template,
    FileUpload,
    i18n,
    withDataRequest
) {
    'use strict';

    defineLumifyAdminPlugin(WorkspaceImport, {
        mixins: [withDataRequest],
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
                this.dataRequest('admin', 'workspaceImport', this.workspaceFile)
                    .then(this.showSuccess.bind(this, i18n('admin.workspace.import.success')))
                    .then(function() {
                        self.trigger(self.select('uploadSelector'), 'reset');
                    })
                    .catch(this.showError.bind(this, i18n('admin.workspace.import.error')))
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
