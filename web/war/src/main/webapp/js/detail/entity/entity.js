
define([
    'flight/lib/component',
    'data',
    './image/image',
    '../properties/properties',
    '../withTypeContent',
    '../withHighlighting',
    'tpl!./entity',
    'tpl!./relationships',
    'tpl!util/alert',
    'util/vertex/list',
    'util/vertex/formatters',
    'detail/dropdowns/propertyForm/propForm',
    'service/ontology',
    'service/vertex',
    'service/config',
    'sf',
    'd3'
], function(defineComponent,
    appData,
    Image,
    Properties,
    withTypeContent,
    withHighlighting,
    template,
    relationshipsTemplate,
    alertTemplate,
    VertexList,
    F,
    PropertyForm,
    OntologyService,
    VertexService,
    ConfigService,
    sf,
    d3) {
    'use strict';

    var MAX_RELATIONS_TO_DISPLAY = 5,
        ontologyService = new OntologyService(),
        vertexService = new VertexService(),
        configService = new ConfigService();

    return defineComponent(Entity, withTypeContent, withHighlighting);

    function defaultSort(x,y) {
        return x === y ? 0 : x < y ? -1 : 1;
    }

    function Entity(withDropdown) {

        this.defaultAttrs({
            glyphIconSelector: '.entity-glyphIcon',
            propertiesSelector: '.properties',
            relationshipsSelector: 'section .relationships',
            titleSelector: '.entity-title',
            relationshipsHeaderSelector: 'section.collapsible h1'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on('click', {
                relationshipsHeaderSelector: this.onToggleRelationships
            });

            this.loadEntity();
        });

        this.onToggleRelationships = function(event) {
            var self = this,
                $section = $(event.target).closest('.collapsible'),
                $content = $section.children('div'),
                $badge = $section.find('.badge');

            if ($section.hasClass('expanded')) {
                return $section.removeClass('expanded');
            }

            $badge.addClass('loading');

            self.handleCancelling(
                vertexService.getVertexRelationships(
                    this.attr.data.id,
                    { offset: 0, size: MAX_RELATIONS_TO_DISPLAY },
                    $section.data('label')
                )
            )
                .always(function() {
                    $badge.removeClass('loading');
                    $section.addClass('expanded');
                })
                .fail(function() {
                    $content.html(alertTemplate({
                        error: i18n('detail.entity.relationships.error')
                    }));
                })
                .done(function(result) {
                    var relationships = result.relationships;

                    if (!relationships.length) {
                        $content.html(alertTemplate({
                            message: i18n('detail.entity.relationships.none_found')
                        }));
                        return;
                    }

                    var node = $content.empty().append('<div>').find('div'),
                        trimmedVertices = _.pluck(relationships, 'vertex').slice(
                            0, MAX_RELATIONS_TO_DISPLAY
                        );

                    node.teardownComponent(VertexList);
                    VertexList.attachTo(node, {
                        vertices: trimmedVertices
                    });

                    if (result.relationships.length !== trimmedVertices.length) {
                        $('<p>')
                            .addClass('paging')
                            .text(i18n(
                                'detail.entity.relationships.paging',
                                F.number.pretty(MAX_RELATIONS_TO_DISPLAY),
                                F.number.pretty(relationships.length)
                            ))
                            .appendTo($content);
                    }
                });
        };

        this.onVerticesUpdated = function(event, data) {
            var matching = _.findWhere(data.vertices, { id: this.attr.data.id });

            if (matching) {
                $('<div>')
                    .addClass('subtitle')
                    .text(matching.concept.displayName)
                    .appendTo(
                        this.select('titleSelector').text(F.vertex.title(matching))
                    )

                this.attr.data = matching;
                this.updateRelationships();
            }
        };

        this.loadEntity = function() {
            var self = this,
                vertexRefresh = this.handleCancelling(appData.refresh(this.attr.data));

            this.vertexRefresh = vertexRefresh;

            vertexRefresh
                .done(function(vertex) {
                    self.vertex = vertex;
                    self.$node.html(template({
                        vertex: vertex,
                        fullscreenButton: self.fullscreenButton([vertex.id]),
                        auditsButton: self.auditsButton(),
                        F: F
                    }));

                    Image.attachTo(self.select('glyphIconSelector'), {
                        data: vertex,
                        service: vertexService
                    });

                   Properties.attachTo(self.select('propertiesSelector'), {
                       data: vertex
                   });

                   self.updateRelationships();
                   self.updateEntityAndArtifactDraggables();
                });
        };

        this.updateRelationships = function() {
            var self = this;

            $.when(
                this.handleCancelling(configService.getProperties()),
                this.handleCancelling(ontologyService.relationships())
            ).done(function(config, relationships) {
                    var hasEntityLabel = config['ontology.iri.artifactHasEntity'],
                        relations = _.map(self.attr.data.edgeLabels, function(label) {
                            var relation = {
                                    label: label,
                                    displayName: label
                                },
                                ontologyRelationship = relationships.byTitle[label];

                            if (label === hasEntityLabel) {
                                relation.displayName = i18n('detail.entity.relationships.has_entity');
                            } else if (ontologyRelationship) {
                                relation.displayName = ontologyRelationship.displayName;
                            }

                            return relation;
                        });

                    d3.select(self.$node.find('.nav-with-background').get(0))
                        .selectAll('section.collapsible')
                        .data(relations, _.property('label'))
                        .call(function() {
                            this.enter()
                                .append('section')
                                .attr('class', 'collapsible')
                                .call(function() {
                                    this.append('h1')
                                        .call(function() {
                                            this.append('strong');
                                            this.append('span').attr('class', 'badge');
                                        });
                                    this.append('div');
                                });

                            this
                                .sort(function(a, b) {
                                    var aIsReference = a.label === hasEntityLabel,
                                        bIsReference = b.label === hasEntityLabel,
                                        nameA = a.displayName, nameB = b.displayName;

                                    if (aIsReference && !bIsReference) return 1;
                                    if (bIsReference && !aIsReference) return -1;

                                    return a.displayName.toLowerCase().localeCompare(b.displayName.toLowerCase());
                                })
                                .attr('data-label', _.property('label'))
                                .select('h1 strong').text(_.property('displayName'));
                        })
                        .exit().remove();
                });
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if (!$target.is('.add-new-properties,button') &&
                $target.parents('.underneath').length === 0) {
                PropertyForm.teardownAll();
            }

            if ($target.is('.entity, .artifact')) {
                var id = $target.data('vertexId');
                this.trigger('selectObjects', { vertices: [appData.vertex(id)] });
                evt.stopPropagation();
            } else if ($target.is('.relationship')) {
                var info = $target.data('info');
                if (info) {
                    this.trigger('selectObjects', { vertices: [info] });
                }
                evt.stopPropagation();
            }
        };

    }
});
