
define([
    'flight/lib/component',
    'data',
    './image/image',
    '../properties/properties',
    '../withTypeContent',
    '../withHighlighting',
    '../toolbar/toolbar',
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
    Toolbar,
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

    var MAX_RELATIONS_TO_DISPLAY,
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
            titleSelector: '.entity-title',
            toolbarSelector: '.comp-toolbar',
            relationshipsHeaderSelector: '.relationships section.collapsible h1',
            relationshipsPagingButtonsSelector: 'section.collapsible .paging button'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            var self = this;
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on('click', {
                relationshipsHeaderSelector: this.onToggleRelationships,
                relationshipsPagingButtonsSelector: this.onPageRelationships
            });
            this.on('addImage', this.onAddImage);

            this.loadEntity();
        });

        this.onAddImage = function(event, data) {
            this.select('glyphIconSelector').trigger('setImage', data);
        };

        this.onToggleRelationships = function(event) {
            var $section = $(event.target).closest('.collapsible');

            if ($section.hasClass('expanded')) {
                return $section.removeClass('expanded');
            }

            this.requestRelationships($section.data({
                offset: 0,
                size: MAX_RELATIONS_TO_DISPLAY
            }));
        };

        this.requestRelationships = function($section) {
            var self = this,
                $content = $section.children('div'),
                $badge = $section.find('.badge'),
                paging = _.pick($section.data(), 'offset', 'size');

            $badge.addClass('loading');

            this.handleCancelling(
                vertexService.getVertexRelationships(
                    this.attr.data.id,
                    paging,
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

                    $badge.text(_.isNumber(result.totalReferences) ?
                        F.number.prettyApproximate(result.totalReferences) : '');
                    $section.data('total', result.totalReferences);

                    var node = $content.empty().append('<div>').find('div'),
                        relationDirections = {},
                        vertices = _.map(relationships, function(relationship) {
                            var relation = relationship.relationship,
                                vertex = relationship.vertex;
                            relationDirections[vertex.id] = 'relation-' + (
                                relation.destVertexId === vertex.id ?
                                    'to' : 'from'
                            );
                            return vertex;
                        });

                    node.teardownComponent(VertexList);
                    VertexList.attachTo(node, {
                        vertices: vertices,
                        relationDirections: relationDirections
                    });

                    if (result.relationships.length !== result.totalReferences) {
                        $('<p>')
                            .addClass('paging')
                            .text(i18n(
                                'detail.entity.relationships.paging',
                                F.number.pretty(paging.offset / paging.size + 1),
                                F.number.pretty(Math.ceil(result.totalReferences / paging.size))
                            ))
                            .append('<button class="previous">')
                            .append('<button class="next">')
                            .appendTo($content)
                    }
                });
        };

        this.onPageRelationships = function(event) {
            var $target = $(event.target),
                isNext = $target.hasClass('next'),
                $section = $target.closest('section'),
                paging = $section.data(),
                previousOffset = paging.offset;

            if (isNext) {
                if (paging.offset + paging.size < paging.total) {
                    paging.offset += paging.size;
                }
            } else {
                if (paging.offset - paging.size >= 0) {
                    paging.offset -= paging.size;
                }
            }

            if (previousOffset !== paging.offset) {
                this.requestRelationships($section);
            }
        };

        this.onVerticesUpdated = function(event, data) {
            var matching = _.findWhere(data.vertices, { id: this.attr.data.id });

            if (matching) {
                this.select('titleSelector').text(F.vertex.title(matching))
                    .next('.subtitle')
                    .text(matching.concept.displayName);

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
                    self.attr.data = vertex;
                    self.$node.html(template({
                        vertex: vertex,
                        F: F
                    }));

                    Toolbar.attachTo(self.select('toolbarSelector'), {
                        toolbar: [
                            {
                                title: i18n('detail.toolbar.open'),
                                submenu: [
                                    Toolbar.ITEMS.FULLSCREEN,
                                    self.sourceUrlToolbarItem()
                                ]
                            },
                            {
                                title: i18n('detail.toolbar.add'),
                                cls: 'requires-EDIT',
                                submenu: [
                                    Toolbar.ITEMS.ADD_PROPERTY,
                                    Toolbar.ITEMS.ADD_IMAGE
                                ]
                            },
                            Toolbar.ITEMS.AUDIT
                        ]
                    });

                    Image.attachTo(self.select('glyphIconSelector'), {
                        data: vertex,
                        service: vertexService
                    });

                   Properties.attachTo(self.select('propertiesSelector'), {
                       data: vertex
                   });

                   self.updateRelationships();
                   self.updateEntityAndArtifactDraggables();
                   self.updateText();
                });
        };

        this.updateRelationships = function() {
            var self = this;

            $.when(
                this.handleCancelling(configService.getProperties()),
                this.handleCancelling(ontologyService.relationships())
            ).done(function(config, relationships) {
                MAX_RELATIONS_TO_DISPLAY = parseInt(config['vertex.relationships.maxPerSection'], 10);

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

                d3.select(self.$node.find('.nav-with-background .relationships').get(0))
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
