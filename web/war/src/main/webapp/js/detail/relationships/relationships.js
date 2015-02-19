define([
    'flight/lib/component',
    'util/withDataRequest',
    'tpl!util/alert',
    'util/formatters',
    'util/vertex/list',
    'd3'
], function(
    defineComponent,
    withDataRequest,
    alertTemplate,
    F,
    VertexList,
    d3) {
    'use strict';

    var MAX_RELATIONS_TO_DISPLAY;

    return defineComponent(Relationships, withDataRequest);

    function Relationships() {

        this.defaultAttrs({
            relationshipsHeaderSelector: '.relationships section.collapsible h1',
            relationshipsPagingButtonsSelector: 'section.collapsible .paging button'
        });

        this.after('initialize', function() {
            this.$node.empty();

            this.on('click', {
                relationshipsHeaderSelector: this.onToggleRelationships,
                relationshipsPagingButtonsSelector: this.onPageRelationships
            });
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.update();
        });

        this.onVerticesUpdated = function(event, data) {
            var matching = _.findWhere(data.vertices, { id: this.attr.data.id });

            if (matching) {
                this.attr.data = matching;
                this.update();
            }
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
                .catch(function(e) {
                    console.error(e);
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

        this.update = function() {
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
                            relation.displayName =
                                self.attr.hasEntityLabel ||
                                i18n('detail.entity.relationships.has_entity');
                        } else if (ontologyRelationship) {
                            relation.displayName = ontologyRelationship.displayName;
                        }

                        return relation;
                    });

                d3.select(self.node)
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
    }
});
