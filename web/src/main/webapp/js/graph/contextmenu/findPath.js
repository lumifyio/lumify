define([
    'util/retina',
    'service/graph'
], function (retina, GraphService) {
    'use strict';

    return FindPath;

    function FindPath() {

        if (!this.graphService) {
            this.graphService = new GraphService();
        }

        this.onContextMenuFindShortestPath = function (hops) {
            var menu = this.select('vertexContextMenuSelector');
            var graphVertexId = menu.data('currentVertexGraphVertexId');
            this.ignoreCySelectionEvents = true;

            this.cy(function (cy) {
                var self = this;
                var sourceVertex = cy.getElementById(graphVertexId);
                var title = sourceVertex.data('title');
                var beginText = 'Select item to find path to "' + title + '"';
                var instructions = $('<div>')
                    .text(beginText)
                    .addClass('instructions')
                    .appendTo(this.$node);
                var edge = null;
                var targetGraphId = null;

                var complete = function () {
                    cy.off(tapEvents);
                    cy.off(mouseEvents);
                    cy.panningEnabled(true)
                        .zoomingEnabled(true)
                        .boxSelectionEnabled(true);
                    self.findingPath = false;

                    if (!edge) {
                        instructions.remove();
                        return;
                    }

                    edge.addClass('label');
                    edge.data('label', 'Finding Path...');

                    var parameters = {
                        sourceGraphVertexId: graphVertexId,
                        destGraphVertexId: targetGraphId,
                        depth: 5,
                        hops: hops
                    };

                    self.graphService.findPath(parameters)
                        .done(function (data) {
                            self.ignoreCySelectionEvents = false;

                            if (edge) {
                                cy.remove(edge);
                                edge = null;
                            }

                            console.log('findPath results', data);

                            var vertices = [];
                            data.paths.forEach(function (path) {
                                path.forEach(function (vertex) {
                                    vertices.push(vertex);
                                });
                            });
                            if (vertices.length === 0) {
                                // TODO: refactor this to some common function on graph
                                var instructions = $('<div>')
                                    .text(beginText)
                                    .addClass('instructions')
                                    .appendTo(self.$node);
                                instructions.text('Could not find a path.');
                            } else {
                                cy.filter(':selected').unselect();
                                cy.container().focus();
                                self.trigger(document, 'addVertices', { vertices: vertices });
                            }
                        })
                        .fail(function() {
                            self.ignoreCySelectionEvents = false;
                        });
                };

                var mouseEvents = {
                    mouseover: function (event) {
                        if (event.cy == event.cyTarget) return;
                        if (event.cyTarget.id() === graphVertexId) return;
                        if (!event.cyTarget.is('node')) return;

                        targetGraphId = event.cyTarget.id();
                        instructions.text('Click to find path');

                        edge = cy.add({
                            group: 'edges',
                            classes: 'temp',
                            data: {
                                source: graphVertexId,
                                target: targetGraphId
                            }
                        });
                    },
                    mouseout: function (event) {
                        if (edge && !edge.hasClass('label')) {
                            cy.remove(edge);
                            edge = null;
                        }
                        instructions.text(beginText);
                    }
                };

                var tapEvents = {
                    tap: function (event) {
                        complete();
                    },
                    cxttap: function (event) {
                        complete();
                    }
                };

                cy.on(mouseEvents);
                cy.on(tapEvents);
            });
        };
    }
});
