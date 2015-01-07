require([
    'configuration/admin/plugin',
    'configuration/admin/utils/fileUpload',
    'hbs!io/lumify/web/devTools/templates/ontology-upload',
    'util/formatters',
    'util/withDataRequest',
    'd3'
], function(
    defineLumifyAdminPlugin,
    FileUpload,
    template,
    F,
    withDataRequest,
    d3
    ) {
    'use strict';

    return defineLumifyAdminPlugin(OntologyUpload, {
        mixins: [withDataRequest],
        section: 'Ontology',
        name: 'Upload',
        subtitle: 'Upload new ontology owl'
    });

    function OntologyUpload() {

        this.defaultAttrs({
            uploadSelector: '.btn-primary',
            iriSelector: 'input.documentIri',
            tokenSelector: '.token'
        });

        this.after('initialize', function() {
            var self = this;

            this.on('fileChanged', this.onFileChanged)
            this.on('click', {
                uploadSelector: this.onUpload
            });
            this.on('change keyup', {
                iriSelector: this.onChange
            });

            this.$node.html(template({}));

            FileUpload.attachTo(this.$node.find('.upload'));
        });

        this.onUpload = function() {
            var self = this,
                importButton = this.select('uploadSelector'),
                request = this.dataRequest('admin', 'ontologyUpload', this.documentIri || '', this.ontologyFile);

            this.handleSubmitButton(importButton, request);

            request.then(function() {
                        self.showSuccess('Upload successful');
                        self.trigger(importButton, 'reset');
                        self.$node.find('.documentIri').val('');
                    })
                    .catch(this.showError.bind(this, 'Upload failed'));
        };

        this.onFileChanged = function(event, data) {
            this.ontologyFile = data.file;
            this.checkValid();
        };

        this.onChange = function() {
            this.documentIri = $.trim(this.$node.find('.documentIri').val());
            this.checkValid();
        };

        this.checkValid = function() {
            if (this.ontologyFile) {
                this.select('uploadSelector').removeAttr('disabled');
            } else {
                this.select('uploadSelector').attr('disabled', true);
            }
        }
    }
});