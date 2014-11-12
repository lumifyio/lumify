
define([
    'flight/lib/component',
    'util/ontology/conceptSelect',
    'util/withFormFieldErrors',
    'data',
    '../withPopover'
], function(
    defineComponent,
    ConceptSelector,
    withFormFieldErrors,
    appData,
    withPopover) {
    'use strict';

    return defineComponent(AddRelatedPopover, withPopover, withFormFieldErrors);

    function AddRelatedPopover() {

        this.defaultAttrs({
            addButtonSelector: '.add',
            searchButtonSelector: '.search',
            cancelButtonSelector: '.cancel',
            promptAddButtonSelector: '.prompt-add',
        });

        this.before('initialize', function(node, config) {
            if (!config.title) {
                console.warn('title attribute required');
                config.title = i18n('popovers.load_related.title_unknown');
            }
            config.template = 'addRelated/template';
        });

        this.after('initialize', function() {
            var self = this;

            this.after('setupWithTemplate', function() {
                this.on(this.popover, 'conceptSelected', this.onConceptSelected);
                this.on(this.popover, 'click', {
                    addButtonSelector: this.onAdd,
                    cancelButtonSelector: this.onCancel,
                    searchButtonSelector: this.onSearch,
                    promptAddButtonSelector: this.onPromptAdd
                })

                appData.refresh(this.attr.relatedToVertexId)
                    .done(function(vertex) {
                        var conceptId = vertex.concept.id;
                        ConceptSelector.attachTo(self.popover.find('.concept'), {
                            defaultText: i18n('popovers.add_related.concept.default_text'),
                            limitRelatedToConceptId: conceptId
                        });
                    })

                this.positionDialog();
            });
        });

        this.onConceptSelected = function(event, data) {
            this.conceptId = data.concept && data.concept.id;

            var searchButton = this.popover.find('.search').hide(),
                promptAdd = this.popover.find('.prompt-add').hide(),
                cancelButton = this.popover.find('.cancel').show(),
                addButton = this.popover.find('.add');

            if (this.relatedRequest && this.relatedRequest.abort) {
                this.relatedRequest.abort();
            }
            this.clearFieldErrors(this.popover);
            searchButton.hide();
            promptAdd.hide();
            cancelButton.hide();
            addButton.show();
        }

        this.onSearch = function(event) {
            this.trigger(document, 'searchByRelatedEntity', {
                vertexId: this.attr.relatedToVertexId,
                conceptId: this.conceptId
            });
            this.teardown();
        };

        this.onPromptAdd = function(event) {
            this.trigger(document, 'addVertices', {
                options: {
                    addingVerticesRelatedTo: this.attr.relatedToVertexId
                },
                vertices: this.promptAddVertices
            });
            this.teardown();
        };

        this.onCancel = function() {
            if (this.relatedRequest) {
                this.relatedRequest.abort();
            }
        }

        this.onAdd = function(event) {
            var self = this,
                searchButton = this.popover.find('.search').hide(),
                promptAdd = this.popover.find('.prompt-add').hide(),
                cancelButton = this.popover.find('.cancel').show(),
                button = $(event.target).addClass('loading').attr('disabled', true);

            $.when(
                configService.getProperties(),
                (
                    this.relatedRequest = vertexService.getRelatedVertices({
                        graphVertexId: this.attr.relatedToVertexId,
                        limitParentConceptId: this.conceptId
                    })
                )
            ).always(function() {
                button.removeClass('loading').removeAttr('disabled');
                searchButton.hide();
                promptAdd.hide();
                cancelButton.hide();
                self.clearFieldErrors(this.popover);
            }).fail(function() {
                self.markFieldErrors(i18n('popovers.add_related.error'));
            }).done(function(config, relatedResponse) {
                var related = relatedResponse[0],
                    count = related.count,
                    vertices = related.vertices,
                    forceSearch = count > config['vertex.loadRelatedMaxForceSearch'],
                    promptBeforeAdding = count > config['vertex.loadRelatedMaxBeforePrompt'];

                if (count === 0) {
                    self.markFieldErrors(i18n('popovers.add_related.no_vertices'), self.popover);
                } else if (forceSearch) {
                    self.markFieldErrors(i18n('popovers.add_related.too_many'), self.popover);
                    button.hide();
                    searchButton.show();
                } else if (promptBeforeAdding) {
                    button.hide();
                    searchButton.show();
                    self.promptAddVertices = vertices;
                    promptAdd.text(i18n('popovers.add_related.button.prompt_add', count)).show();
                } else {
                    self.trigger(document, 'addVertices', {
                        options: {
                            addingVerticesRelatedTo: self.attr.relatedToVertexId
                        },
                        vertices: vertices
                    });
                    self.teardown();
                }
            })

        };
    }
});
