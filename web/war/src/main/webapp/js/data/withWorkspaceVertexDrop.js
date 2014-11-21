
define([], function() {
    'use strict';

    return withWorkspaceVertexDrop;

    function withWorkspaceVertexDrop() {

        this.after('setupDataWorker', function() {
            var self = this,
                enabled = false,
                droppable = $(document.body);

            // Other droppables might be on top of graph, listen to
            // their over/out events and ignore drops if the user hasn't
            // dragged outside of them. Can't use greedy option since they are
            // absolutely positioned
            $(document.body).on('dropover dropout', function(e, ui) {
                var target = $(e.target),
                    appDroppable = target.is(droppable),
                    parentDroppables = target.parents('.ui-droppable');

                if (appDroppable) {
                    // Ignore events from this droppable
                    return;
                }

                // If this droppable has no parent droppables
                if (parentDroppables.length === 1 && parentDroppables.is(droppable)) {
                    enabled = e.type === 'dropout';
                }
            });

            droppable.droppable({
                tolerance: 'pointer',
                accept: function(item) {
                    return true;
                },
                over: function(event, ui) {
                    var draggable = ui.draggable,
                        start = true,
                        graphVisible = $('.graph-pane-2d').is('.visible'),
                        dashboardVisible = $('.dashboard-pane').is('.visible'),
                        vertices,
                        started = false,
                        wrapper = $('.draggable-wrapper');

                    // Prevent map from swallowing mousemove events by adding
                    // this transparent full screen div
                    if (wrapper.length === 0) {
                        wrapper = $('<div class="draggable-wrapper"/>').appendTo(document.body);
                    }

                    draggable.off('drag.droppable-tracking');
                    draggable.on('drag.droppable-tracking', function handler(event, draggableUI) {
                        if (!vertices) {
                            if (!started) {
                                started = true;
                                // TODO: for non-cached vertices we need
                                // some ui feedback that it's loading
                                verticesFromDraggable(draggable, self.dataRequestPromise)
                                    .done(function(v) {
                                        vertices = v;
                                        handler(event, draggableUI);
                                    })
                            }
                            return;
                        }

                        if (graphVisible) {
                            ui.helper.toggleClass('draggable-invisible', enabled);
                        } else if (dashboardVisible) {
                            self.trigger('menubarToggleDisplay', { name: 'graph' });
                            dashboardVisible = false;
                            graphVisible = true;
                        }

                        self.trigger('toggleWorkspaceFilter', { enabled: !enabled });
                        if (graphVisible) {
                            if (enabled) {
                                self.trigger('verticesHovering', {
                                    vertices: vertices,
                                    start: start,
                                    position: { x: event.pageX, y: event.pageY }
                                });
                                start = false;
                            } else {
                                self.trigger('verticesHoveringEnded');
                            }
                        }
                    });
                },
                drop: function(event, ui) {
                    $('.draggable-wrapper').remove();

                    // Early exit if should leave to a different droppable
                    if (!enabled) return;

                    verticesFromDraggable(ui.draggable, self.dataRequestPromise)
                        .done(function(vertices) {
                            var graphVisible = $('.graph-pane-2d').is('.visible');

                            // TODO: workspace editable?
                            self.trigger('clearWorkspaceFilter');
                            self.trigger('verticesDropped', {
                                vertices: vertices,
                                dropPosition: { x: event.clientX, y: event.clientY }
                            });
                        })
                }.bind(this)
            });

            function verticesFromDraggable(draggable, dataRequestPromise) {
                var alsoDragging = draggable.data('ui-draggable').alsoDragging,
                    anchors = draggable;

                if (alsoDragging && alsoDragging.length) {
                    anchors = draggable.add(alsoDragging.map(function(i, a) {
                        return a.data('original');
                    }));
                }

                var vertexIds = _.compact(anchors.map(function(i, a) {
                    a = $(a);
                    var vertexId = a.data('vertexId') || a.closest('li').data('vertexId');
                    if (a.is('.facebox')) return;

                    if (!vertexId) {

                        // Highlighted entities (legacy info)
                        var info = a.data('info') || a.closest('li').data('info');

                        vertexId = info && (info.resolvedToVertexId || info.graphVertexId || info.id);

                        // Detected objects
                        if (info && info.entityVertex) {
                            self.updateCacheWithVertex(info.entityVertex);
                            vertexId = info.entityVertex.id;
                        }

                        if (!vertexId) {
                            console.error('No data-vertex-id attribute for draggable element found', a[0]);
                            return;
                        }
                    }
                    return vertexId;
                }).toArray());

                return dataRequestPromise.then(function(dataRequest) {
                    return dataRequest('vertex', 'store', { vertexIds: vertexIds });
                });
            }
        });
    }
});
