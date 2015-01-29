
define([
    'flight/lib/component',
    './image/image',
    '../properties/properties',
    '../comments/comments',
    '../withTypeContent',
    '../withHighlighting',
    '../toolbar/toolbar',
    'tpl!./entity',
    'tpl!./relationships',
    'tpl!util/alert',
    'util/vertex/list',
    'util/vertex/formatters',
    'util/withDataRequest',
    'detail/dropdowns/propertyForm/propForm',
    'd3'
], function(defineComponent,
    Image,
    Properties,
    Comments,
    withTypeContent,
    withHighlighting,
    Toolbar,
    template,
    relationshipsTemplate,
    alertTemplate,
    VertexList,
    F,
    withDataRequest,
    PropertyForm,
    d3) {
    'use strict';

    var MAX_RELATIONS_TO_DISPLAY; // Loaded with configuration parameters

    return defineComponent(Entity, withTypeContent, withHighlighting, withDataRequest);

    function defaultSort(x,y) {
        return x === y ? 0 : x < y ? -1 : 1;
    }

    function Entity(withDropdown) {

        this.defaultAttrs({
            glyphIconSelector: '.entity-glyphIcon',
            propertiesSelector: '.properties',
            commentsSelector: '.comments',
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

            this.dataRequest('vertex', 'edges', this.attr.data.id, {
                offset: paging.offset,
                size: paging.size,
                edgeLabel: $section.data('label')
            })
                .then(function(result) {
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
                })
                .catch(function() {
                    $content.html(alertTemplate({
                        error: i18n('detail.entity.relationships.error')
                    }));
                })
                .finally(function() {
                    $badge.removeClass('loading');
                    $section.addClass('expanded');
                })
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
                    .text(F.vertex.concept(matching).displayName);

                this.attr.data = matching;
                this.updateRelationships();
            }
        };

        this.loadEntity = function() {
            var vertex = this.attr.data;

            this.trigger('finishedLoadingTypeContent');

            this.vertex = vertex;
            this.attr.data = vertex;
            this.$node.html(template({
                vertex: vertex,
                F: F
            }));

            Toolbar.attachTo(this.select('toolbarSelector'), {
                toolbar: [
                    {
                        title: i18n('detail.toolbar.open'),
                        submenu: [
                            Toolbar.ITEMS.FULLSCREEN,
                            this.sourceUrlToolbarItem()
                        ]
                    },
                    {
                        title: i18n('detail.toolbar.add'),
                        submenu: [
                            Toolbar.ITEMS.ADD_PROPERTY,
                            Toolbar.ITEMS.ADD_IMAGE,
                            Toolbar.ITEMS.ADD_COMMENT
                        ]
                    },
                    {
                        icon: 'img/glyphicons/white/glyphicons_157_show_lines@2x.png',
                        right: true,
                        submenu: [
                            Toolbar.ITEMS.AUDIT,
                            _.extend(Toolbar.ITEMS.DELETE_ITEM, {
                                title: i18n('detail.toolbar.delete.entity'),
                                subtitle: i18n('detail.toolbar.delete.entity.subtitle')
                            })
                        ]
                    }
                ]
            });

            Image.attachTo(this.select('glyphIconSelector'), {
                data: vertex
            });

            Properties.attachTo(this.select('propertiesSelector'), {
                data: vertex
            });

            Comments.attachTo(this.select('commentsSelector'), {
                vertex: vertex
            });

            this.updateRelationships();
            this.updateEntityAndArtifactDraggables();
            this.updateText();
        };

        this.updateRelationships = function() {
            var self = this;

            Promise.all([
                this.dataRequest('config', 'properties'),
                this.dataRequest('ontology', 'relationships')
            ]).done(function(results) {
                var config = results[0],
                    relationships = results[1];

                MAX_RELATIONS_TO_DISPLAY = parseInt(config['vertex.relationships.maxPerSection'], 10);

                var hasEntityLabel = config['ontology.intent.relationship.artifactHasEntity'],
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
                this.trigger('selectObjects', { vertexIds: [id] });
                evt.stopPropagation();
            }
        };

    }
});
