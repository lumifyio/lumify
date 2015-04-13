
define([
    'flight/lib/component',
    './3djs/3djs',
    'util/vertex/formatters',
    'tpl!util/alert'
], function(defineComponent, $3djs, F, alertTemplate) {
    'use strict';

    var MAX_TITLE_LENGTH = 15,
        imageCache = {};

    return defineComponent(Graph3D);

    function loadImage(src) {
        if (imageCache[src]) {
            return imageCache[src];
        }

        var deferred = $.Deferred(),
            image = new Image();

        image.onload = function() {
            deferred.resolve(this);
        };
        image.onerror = function() {
            deferred.reject(arguments);
        };
        image.src = src;
        imageCache[src] = deferred.promise();
        return imageCache[src];
    }

    function Graph3D() {
        this.defaultAttrs({ });

        this.after('teardown', function() {
            imageCache = {};
            this.graphRenderer.teardown();
            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            this.graph = new $3djs.Graph();
            var loadSuccess = this.load3djs();
            if (loadSuccess) {
                if (this.attr.vertices && this.attr.vertices.length) {
                    this.addVertices(this.attr.vertices);
                }

                this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
                this.on(document, 'verticesAdded', this.onVerticesAdded);
                this.on(document, 'verticesDropped', this.onVerticesDropped);
                this.on(document, 'verticesDeleted', this.onVerticesDeleted);
                this.on(document, 'verticesUpdated', this.onVerticesUpdated);
                this.on(document, 'existingVerticesAdded', this.onExistingVerticesAdded);
                this.on(document, 'edgesLoaded', this.onEdgesLoaded);

                this.on('showPanel', this.onShowPanel);
                this.on('hidePanel', this.onHidePanel);
            } else {
                this.$node.html(alertTemplate({error: i18n('graph.3d.webgl.error')}));
            }
        });

        this.onShowPanel = function() {
            if (this.graphRenderer) {
                this.graphRenderer.running = true;
                this.graphRenderer.continueAnimation();
            }
        };

        this.onHidePanel = function() {
            if (this.graphRenderer) {
                this.graphRenderer.running = false;
            }
        };

        this.onVerticesDropped = function(event, data) {
           if (!this.$node.is(':visible')) return;

           this.trigger(document, 'addVertices', data);
        };

        this.addVertices = function(vertices) {
            var self = this,
                graph = this.graph,
                deferredImages = [];

            vertices.forEach(function(vertex) {
                var node = new $3djs.Graph.Node(vertex.id);

                node.data.vertex = vertex;
                node.data.icon = F.vertex.image(vertex);

                if (node.data.icon) {
                    deferredImages.push(
                        loadImage(node.data.icon)
                            .done(function(image) {
                                var ratio = image.naturalWidth / image.naturalHeight,
                                    height = 150;

                                node.data.icon = image.src;
                                addToGraph(height * ratio, height, node);
                            })
                    );
                } else {
                    console.warn('No icon set for vertex: ', vertex);
                }
            });

            $.when.apply(null, deferredImages).done(function() {
                if (self.relationships && self.relationships.length) {
                    self.addEdges(self.relationships);
                }
                graph.needsUpdate = true;
            });
            function addToGraph(width, height, node) {
                node.data.iconWidth = width;
                node.data.iconHeight = height;

                var title = F.vertex.title(node.data.vertex);
                if (title.length > MAX_TITLE_LENGTH) {
                    node.data.label = $.trim(title.substring(0, MAX_TITLE_LENGTH)) + '...';
                } else node.data.label = title;

                node.needsUpdate = true;
                graph.addNode(node);
            }
        };

        this.onWorkspaceLoaded = function(evt, workspace) {
            var self = this,
                graph = this.graph;

            this.isWorkspaceEditable = workspace.editable;
            if (workspace.data && workspace.data.vertices) {
                this.addVertices(workspace.data.vertices);
            }
        };

        this.onVerticesAdded = function(event, data) {
            if (!this.isWorkspaceEditable) return;

            if (data.vertices) {
                this.addVertices(data.vertices);
            }
        };

        this.onVerticesDeleted = function(event, data) {
            var self = this;

            data.vertexIds.forEach(function(v) {
                self.graph.removeNode(v);
            });

            self.graph.needsUpdate = true;
        };
        this.onVerticesUpdated = function() {
        };
        this.onExistingVerticesAdded = function() {
        };
        this.onEdgesLoaded = function(event, data) {
            var graph = this.graph;

            if (data.edges) {
                this.relationships = data.edges;
                this.addEdges(data.edges);
                this.graph.needsUpdate = true;
            }
        };

        this.addEdges = function(relationships) {
            var graph = this.graph,
                edges = graph.edges;

            edges.length = 0;
            relationships.forEach(function(r) {
                var source = graph.node(r.sourceVertexId),
                    target = graph.node(r.destVertexId);

                if (source && target) {
                    edges.push({
                        source: source,
                        target: target
                    });
                }
            });
        };

        this.load3djs = function() {
            var graph = this.graph,
                self = this,
                graphRenderer = new $3djs.GraphRenderer(this.node);

            if (graphRenderer.browserSupported) {
                this.graphRenderer = graphRenderer;
                graphRenderer.renderGraph(this.graph);
                graphRenderer.addToRenderLoop();
                //graphRenderer.showStats();

                graphRenderer.addEventListener('node_click', function(event) {
                    var selected = [];
                    if (event.content) {
                        var data = graph.node(event.content).data.vertex;
                        selected.push(data);
                    }
                    self.trigger('selectObjects', { vertices: selected });
                }, false);

                return true;
            }

            return false;
        };
    }
});
