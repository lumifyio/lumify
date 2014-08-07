define([
    'flight/lib/component',
    'require',
    'util/messages',
    'util/handlebars/helpers'
], function(
    defineComponent,
    require,
    i18n) {
    'use strict';

    return defineComponent(FileUpload);

    function FileUpload() {

        this.defaultAttrs({
            buttonText: i18n('admin.utils.file-upload.button.select'),
            uploadSelector: '.btn-file',
            uploadInputSelector: 'input',
            removeSelector: '.btn-danger',
            selectedFileSelector: '.selected-file'

            // TODO: add accepted filetypes attribute
        });

        this.after('initialize', function() {
            var self = this;

            this.on('change', {
                uploadInputSelector: this.onFileChange
            });
            this.on('click', {
                removeSelector: this.onRemove
            });
            this.on('reset', this.onRemove);

            require(['hbs!./fileUploadTemplate'], function(tpl) {
                self.$node.html(tpl(self.attr));
            });
        });

        this.onRemove = function(event) {
            this.select('uploadInputSelector').val('');
            this.updateFile();
        }

        this.onFileChange = function(event) {
            var files = event.target.files;

            this.updateFile(files && files.length && files[0]);
        };

        this.updateFile = function(file) {
            var self = this;

            this.trigger('fileChanged', {
                file: file
            });

            if (!file) {
                this.select('removeSelector').hide();
                this.select('uploadSelector').show();
                return this.select('selectedFileSelector').empty();
            }

            require(['hbs!./fileUploadItemTemplate'], function(tpl) {
                self.select('uploadSelector').hide();
                self.select('removeSelector').show();
                self.select('selectedFileSelector').html(tpl({ file: file }));
            });
        };

    }
});
