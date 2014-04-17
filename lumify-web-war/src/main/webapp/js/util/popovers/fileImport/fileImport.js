
define([
    'flight/lib/component',
    '../withPopover',
    'service/vertex',
    'util/formatters',
    'configuration/plugins/visibility/visibilityEditor',
    'util/withFormFieldErrors'
], function(
    defineComponent,
    withPopover,
    VertexService,
    formatters,
    VisibilityEditor,
    withFormFieldErrors) {
    'use strict';

    return defineComponent(FileImport, withPopover, withFormFieldErrors);

    function FileImport() {

        var vertexService = new VertexService();

        this.defaultAttrs({
            buttonSelector: 'button',
            checkboxSelector: '.checkbox',
            visibilityInputSelector: '.visibility'
        });

        this.before('initialize', function(node, config) {
            config.template = 'fileImport/template';
            config.multipleFiles = config.files.length > 1;
            config.formattedFiles = _.map(config.files, function(f) {
                return {
                    name: f.name,
                    size: formatters.bytes.pretty(f.size, 0)
                };
            })
            config.pluralString = formatters.string.plural(config.files.length, 'file');
            //config.teardownOnTap = false;

            this.after('setupWithTemplate', function() {
                var self = this;

                VisibilityEditor.attachTo(this.popover.find('.visibility'));

                this.on(this.popover, 'click', {
                    buttonSelector: this.onImport
                });

                this.on(this.popover, 'change', {
                    checkboxSelector: this.onCheckboxCopy
                })

                this.on(this.popover, 'keyup', {
                    visibilityInputSelector: function(e) {
                        if ($(e.target).closest('.visibility.single').length &&
                            e.which === $.ui.keyCode.ENTER) {
                            this.onImport();
                        }
                    }
                })

                window.focus();
                _.defer(function() {
                    self.popover.find('.visibility input').focus();
                })
            })
        });

        this.onCheckboxCopy = function(e) {
            var $checkbox = $(e.target),
                checked = $checkbox.is(':checked');

            this.popover.toggleClass('collapseVisibility', checked);
            this.popover.find('.errors').empty();
            _.delay(this.positionDialog.bind(this), 50);
        };

        this.onImport = function() {
            var self = this,
                button = this.popover.find('button')
                    .addClass('loading')
                    .attr('disabled', true),
                originalText = button.text();

            this.attr.teardownOnTap = false;

            vertexService.importFiles(this.attr.files)
                .progress(function(complete) {
                    var percent = Math.round(complete * 100);
                    button.text(percent + '% Importing...');
                })
                .fail(function(xhr, m, error) {
                    self.attr.teardownOnTap = true;
                    self.markFieldErrors(error, self.popover);
                    self.positionDialog();
                    button.text(originalText)
                        .removeClass('loading')
                        .removeAttr('disabled')
                })
                .done(function(result) {

                    vertexService.getMultiple(result.vertexIds)
                        .done(function(result) {
                            self.trigger('addVertices', {
                                vertices: result.vertices,
                                options: {
                                    fileDropPosition: self.attr.anchorTo.page
                                }
                            });

                            self.teardown();
                        });
                });
        }
    }
});
