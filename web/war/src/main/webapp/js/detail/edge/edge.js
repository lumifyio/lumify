define([
    'flight/lib/component',
    '../withTypeContent',
    '../withHighlighting',
    '../toolbar/toolbar',
    '../comments/comments',
    'tpl!./edge',
    'detail/properties/properties',
    'util/vertex/formatters',
    'util/withDataRequest',
    'd3'
], function(
    defineComponent,
    withTypeContent,
    withHighlighting,
    Toolbar,
    Comments,
    template,
    Properties,
    F,
    withDataRequest,
    d3) {
    'use strict';

    var predicate = { name: 'http://lumify.io#conceptType' };

    return defineComponent(Edge, withTypeContent, withHighlighting, withDataRequest);

    function Edge() {

        this.defaultAttrs({
            vertexToVertexRelationshipSelector: '.vertex-to-vertex-relationship',
            propertiesSelector: '.properties',
            commentsSelector: '.comments',
            toolbarSelector: '.comp-toolbar'
        });

        this.after('teardown', function() {
            this.$node.off('click.paneClick');
        });

        this.after('initialize', function() {
            this.$node.on('click.paneClick', this.onPaneClicked.bind(this));
            this.on('click', {
                vertexToVertexRelationshipSelector: this.onVertexToVertexRelationshipClicked
            });

            this.on(document, 'verticesUpdated', this.onVerticesUpdated);

            this.loadRelationship();
        });

        this.onVerticesUpdated = function(event, data) {
            var source = _.findWhere(data.vertices, { id: this.edge.source.id }),
                target = _.findWhere(data.vertices, { id: this.edge.target.id });

            if (source) {
                this.edge.source = source;
            }
            if (target) {
                this.edge.target = target;
            }

            if (source || target) {
                this.update();
            }
        };

        this.update = function() {
            var edge = this.edge,
                source = edge.source,
                target = edge.target;

            d3.select(this.node).selectAll('.vertex-to-vertex-relationship')
                .data([source, target])
                .call(function() {
                    this
                        .text(function(d) {
                            return F.vertex.title(d);
                        })
                        .attr('data-vertex-id', function(d) {
                            return d.id;
                        })
                        .append('div')
                        .attr('class', 'subtitle')
                        .text(function(d) {
                            var concept = F.vertex.concept(d);
                            return concept.displayName;
                        });
                })
        };

        this.loadRelationship = function() {
            var self = this,
                data = this.attr.data;

            Promise.all([
                this.dataRequest('ontology', 'ontology'),
                this.dataRequest('edge', 'store', { edgeIds: [data.id] })
            ]).done(function(results) {
                var ontology = results.shift(),
                    edges = results.shift(),
                    edge = edges[0];

                if (!edge) {
                    return self.trigger('selectObjects');
                }

                self.ontologyRelationships = ontology.relationships;
                self.edge = edge;
                $.extend(edge.source, {
                    concept: F.vertex.concept(edge.source)
                });

                $.extend(edge.target, {
                    concept: F.vertex.concept(edge.target)
                });
                self.$node.html(template({}));
                self.update();

                Properties.attachTo(self.select('propertiesSelector'), {
                    data: edge
                });

                Comments.attachTo(self.select('commentsSelector'), {
                    edge: edge
                });

                Toolbar.attachTo(self.select('toolbarSelector'), {
                    toolbar: [
                        {
                            title: i18n('detail.toolbar.add'),
                            submenu: [
                                Toolbar.ITEMS.ADD_COMMENT
                            ]
                        },
                        {
                            icon: 'img/glyphicons/white/glyphicons_157_show_lines@2x.png',
                            right: true,
                            submenu: [
                                Toolbar.ITEMS.AUDIT,
                                _.extend(Toolbar.ITEMS.DELETE_ITEM, {
                                    title: i18n('detail.toolbar.delete.edge'),
                                    subtitle: i18n('detail.toolbar.delete.edge.subtitle')
                                })
                            ]
                        }
                    ]
                });

                self.updateEntityAndArtifactDraggables();
            });
        };

        this.onVertexToVertexRelationshipClicked = function(evt) {
            var $target = $(evt.target),
                id = $target.data('vertexId');
            this.trigger(document, 'selectObjects', { vertexIds: [id] });
        };

        this.onPaneClicked = function(evt) {
            var $target = $(evt.target);

            if ($target.is('.vertex, .artifact, span.relationship')) {
                var id = $target.data('vertexId');
                this.trigger(document, 'selectObjects', { vertexIds: [id] });
                evt.stopPropagation();
            }
        };
    }
});
