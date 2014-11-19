define([], function() {
    'with strict';

    var STATE_NONE = 0,
        STATE_STARTED = 1,
        STATE_CONNECTED = 2;

    return withControlDrag;

    function withControlDrag() {
        var controlKeyPressed = false,
            startControlDragTarget,
            tempTargetNode,
            currentEdgeId,
            state = STATE_NONE,
            connectionData,
            connectionType;

        this.after('initialize', function() {
            var self = this,
                lockedCyTarget;

            this.mouseDragHandler = self.onControlDragMouseMove.bind(this);

            this.on(document, 'controlKey', function() {
                controlKeyPressed = true;
            });
            this.on(document, 'controlKeyUp', function() {
                controlKeyPressed = false;
            });
            this.on(document, 'mouseup', function() {
                if (state === STATE_STARTED) {
                    self.trigger('endVertexConnection', {
                        edgeId: currentEdgeId
                    });
                }
            });

            this.on('startVertexConnection', this.onStartVertexConnection);
            this.on('endVertexConnection', this.onEndVertexConnection);
            this.on('finishedVertexConnection', this.onFinishedVertexConnection);
            this.on('selectObjects', this.onSelectObjects);

            this.cytoscapeReady(function(cy) {
                var mousedown = function(event) {
                    if (state > STATE_NONE) return;
                    if (controlKeyPressed && event.cyTarget !== cy) {
                        self.trigger('startVertexConnection', {
                            vertexId: self.fromCyId(event.cyTarget.id())
                        });
                    }
                };

                cy.on({
                    tap: function(event) {
                        if (state === STATE_CONNECTED) {
                            self.trigger('finishedVertexConnection');
                        }
                    },
                    cxttapstart: mousedown,
                    tapstart: mousedown,
                    grab: function(event) {

                        if (controlKeyPressed) {
                            if (state > STATE_NONE) {
                                self.trigger('finishedVertexConnection');
                            }
                            lockedCyTarget = event.cyTarget;
                            lockedCyTarget.lock();
                        }
                    }
                });
            });
        });

        this.onSelectObjects = function(e) {

            if (state > STATE_NONE) {
                e.stopPropagation();
            }

            if (state === STATE_CONNECTED) {
                this.trigger('finishedVertexConnection');
            }
        };

        this.onFinishedVertexConnection = function(event) {
            var self = this,
                resetCytoscape = function() {
                    state = STATE_NONE;
                    self.cytoscapeReady(function(cy) {
                        cy.$('.temp').remove();
                        cy.$('.controlDragSelection').removeClass('controlDragSelection');
                        currentEdgeId = null;
                        currentSourceId = null;
                        currentTargetId = null;

                        self.ignoreCySelectionEvents = false;
                        self.trigger('defocusPaths');
                    });
                }

            require(['graph/popovers/withVertexPopover', 'flight/lib/registry'], function(withVertexPopover, registry) {
                var popovers = self.$node.lookupAllComponentsWithMixin(withVertexPopover);
                if (popovers.length === 0 || popovers[0].attr.teardownOnTap !== false) {
                    resetCytoscape();
                    _.delay(function() {
                        popovers.forEach(function(p) {
                            if (registry.findInstanceInfo(p)) {
                                p.teardown();
                            }
                        })
                    }, 500)
                }
            });
        };

        this.onStartVertexConnection = function(event, data) {
            state = STATE_STARTED;
            connectionType = data.connectionType;
            connectionData = _.omit(data, 'connectionType');

            this.ignoreCySelectionEvents = true;

            this.cytoscapeReady(function(cy) {
                startControlDragTarget = cy.getElementById(this.toCyId(data.vertexId));
                cy.nodes().lock();
                cy.on('mousemove', this.mouseDragHandler);
            });
        };

        this.onEndVertexConnection = function(event, data) {
            var self = this;

            this.cytoscapeReady(function(cy) {
                cy.off('mousemove', this.mouseDragHandler);
                cy.nodes().unlock();
                startControlDragTarget = null;

                var edge = currentEdgeId && cy.getElementById(currentEdgeId),
                    target = edge && cy.getElementById(edge.data('target')),
                    other = edge && cy.getElementById(edge.data('source'));

                if (!target || target.hasClass('temp')) {
                    return this.trigger('finishedVertexConnection');
                }

                var componentName = {
                    CreateConnection: 'createConnectionPopover',
                    FindPath: 'findPathPopover'
                }[connectionType] ||   'controlDragPopover';

                require(['graph/popovers/' + componentName], function(Popover) {
                    Popover.teardownAll();
                    Popover.attachTo(self.$node, {
                        cy: cy,
                        cyNode: target,
                        otherCyNode: other,
                        edge: edge,
                        sourceVertexId: self.fromCyId(edge.data('source')),
                        targetVertexId: self.fromCyId(edge.data('target')),
                        connectionData: connectionData
                    });
                    state = STATE_CONNECTED;
                });
            });
        };

        this.onControlDragMouseMove = function(event) {
            var cy = event.cy,
                target = event.cyTarget !== cy && event.cyTarget.is('node') ? event.cyTarget : null,
                targetIsSource = target === startControlDragTarget;

            cy.$('.controlDragSelection').removeClass('controlDragSelection');

            if (!target) {

                var oe = event.originalEvent || event,
                    pageX = oe.pageX,
                    pageY = oe.pageY,
                    projected = cy.renderer().projectIntoViewport(pageX, pageY),
                    position = {
                        x: projected[0],
                        y: projected[1]
                    };

                if (!tempTargetNode) {
                    tempTargetNode = cy.add({
                        group: 'nodes',
                        classes: 'temp',
                        data: {
                            id: 'controlDragNodeId'
                        },
                        position: position
                    });
                } else {
                    if (tempTargetNode.removed) {
                        tempTargetNode.restore();
                    }
                    tempTargetNode.position(position);
                }

                createTempEdge(tempTargetNode.id());

            } else if (target && !targetIsSource) {
                target.addClass('controlDragSelection');
                createTempEdge(target.id());
            }

            function createTempEdge(targetId) {
                var sourceId = startControlDragTarget.id(),
                    edgeId = sourceId + '-' + targetId,
                    tempEdges = cy.$('edge.temp'),
                    edge = cy.getElementById(edgeId);

                tempEdges.remove();
                if (edge.removed) {
                    edge.restore();
                }

                currentEdgeId = edgeId;
                currentSourceId = sourceId;
                currentTargetId = targetId;
                if (!edge.length) {
                    cy.add({
                        group: 'edges',
                        classes: 'temp',
                        data: {
                            id: edgeId,
                            source: sourceId,
                            target: targetId
                        }
                    });
                }
            }
        };
    }
});
