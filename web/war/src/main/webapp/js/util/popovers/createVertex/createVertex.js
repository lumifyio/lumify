
define([
    'flight/lib/component',
    '../withPopover',
    'service/vertex',
    'util/formatters',
    'configuration/plugins/visibility/visibilityEditor',
    'util/withFormFieldErrors',
    'util/ontology/conceptSelect'
], function(
    defineComponent,
    withPopover,
    VertexService,
    F,
    VisibilityEditor,
    withFormFieldErrors,
    ConceptSelect) {
    'use strict';

    return defineComponent(CreateVertex, withPopover, withFormFieldErrors);

    function CreateVertex() {

        var vertexService = new VertexService();

        this.defaultAttrs({
            createSelector: '.btn-primary',
            cancelSelector: '.btn-default',
            visibilityInputSelector: '.visibility'
        });

        this.after('teardown', function() {
            if (this.request) {
                this.request.abort();
            }
        });

        this.before('initialize', function(node, config) {
            config.template = 'createVertex/template';
            config.teardownOnTap = false;

            this.after('setupWithTemplate', function() {
                var self = this;

                this.visibilitySource = null;

                this.on(this.popover, 'visibilitychange', this.onVisibilityChange);

                ConceptSelect.attachTo(this.popover.find('.concept'));
                VisibilityEditor.attachTo(this.popover.find('.visibility'));

                this.on(this.popover, 'click', {
                    createSelector: this.onCreate,
                    cancelSelector: this.onCancel
                });
                this.on(this.popover, 'conceptSelected', this.onConceptSelected);

                window.focus();
            })
        });

        this.onConceptSelected = function(event, data) {
            this.concept = data.concept;
            this.checkValid();
        };

        this.onVisibilityChange = function(event, data) {
            this.visibilitySource = data;
            this.checkValid();
        };

        this.checkValid = function() {
            var isValid = this.visibilitySource &&
                this.visibilitySource.valid &&
                this.concept;

            if (isValid) {
                this.popover.find('.btn-primary').removeAttr('disabled');
            } else {
                this.popover.find('.btn-primary').attr('disabled', true);
            }

            return isValid;
        };

        this.onCancel = function() {
            this.teardown();
        };

        this.onCreate = function() {
            if (!this.checkValid()) {
                return false;
            }

            var self = this,
                button = this.popover.find('.btn-primary')
                    .text(i18n('popovers.create_vertex.button.creating'))
                    .attr('disabled', true),
                conceptType = this.concept.id,
                visibilityValue = this.visibilitySource.value;

            this.request = vertexService.createVertex(conceptType, visibilityValue)
                .fail(function(xhr, m, error) {
                    self.markFieldErrors(error, self.popover);
                    button.text(i18n('popovers.create_vertex.button.create'))
                        .removeClass('loading')
                        .removeAttr('disabled')

                    _.defer(self.positionDialog.bind(self));
                })
                .done(function(result) {
                    self.trigger('addVertices', {
                        vertices: [ result ],
                        options: {
                            fileDropPosition: self.attr.anchorTo.page
                        }
                    });

                    self.teardown();
                });
        }
    }
});
