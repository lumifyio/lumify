
define([
    'flight/lib/component',
    'cytoscape',
    './renderer',
    './stylesheet',
    './withControlDrag',
    'tpl!./graph',
    'tpl!./loading',
    'util/controls',
    'util/throttle',
    'util/vertex/formatters',
    'util/privileges',
    'util/retina',
    'util/withContextMenu',
    'util/withAsyncQueue',
    'util/withDataRequest',
    'configuration/plugins/exportWorkspace/plugin',
    'colorjs'
], function(
    defineComponent,
    cytoscape,
    Renderer,
    stylesheet,
    withControlDrag,
    template,
    loadingTemplate,
    Controls,
    throttle,
    F,
    Privileges,
    retina,
    withContextMenu,
    withAsyncQueue,
    withDataRequest,
    WorkspaceExporters,
    colorjs) {
    'use strict';

        // Delay before showing hover effect on graph
    var HOVER_FOCUS_DELAY_SECONDS = 0.25,
        MAX_TITLE_LENGTH = 15,
        SELECTION_THROTTLE = 100,
        // How many edges are required before we don't show them on zoom/pan
        SHOW_EDGES_ON_ZOOM_THRESHOLD = 50,
        GRID_LAYOUT_X_INCREMENT = 175,
        GRID_LAYOUT_Y_INCREMENT = 100;

    return defineComponent(Graph, withAsyncQueue, withContextMenu, withControlDrag, withDataRequest);

    function Graph() {

        var LAYOUT_OPTIONS = {
                // Customize layout options
                random: { padding: 10 },
                arbor: { friction: 0.6, repulsion: 5000 * retina.devicePixelRatio, targetFps: 60, stiffness: 300 }
            },
            fromCyId = function(cyId) {
                return F.className.from(cyId);
            },
            toCyId = function(v) {
                var vId = _.isString(v) ? v : v.id;
                return F.className.to(vId);
            };

        this.toCyId = toCyId;
        this.fromCyId = fromCyId;

        this.defaultAttrs({
            cytoscapeContainerSelector: '.cytoscape-container',
            emptyGraphSelector: '.empty-graph',
            graphToolsSelector: '.controls',
            contextMenuSelector: '.graph-context-menu',
            vertexContextMenuSelector: '.vertex-context-menu',
            edgeContextMenuSelector: '.edge-context-menu'
        });

        this.onVerticesHoveringEnded = function(evt, data) {
            this.cytoscapeReady(function(cy) {
                cy.$('.hover').remove();
            });
        };

        var vertices, idToCyNode;
        this.onVerticesHovering = function(evt, data) {
            if (!this.isWorkspaceEditable) {
                return this.trigger('displayInformation', { message: i18n('graph.workspace.readonly') })
            }
            this.cytoscapeReady(function(cy) {
                var self = this,
                    offset = this.$node.offset(),
                    renderedPosition = retina.pointsToPixels({
                        x: data.position.x - offset.left,
                        y: data.position.y - offset.top
                    }),
                    start = {
                        x: renderedPosition.x,
                        y: renderedPosition.y
                    },
                    inc = GRID_LAYOUT_X_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    yinc = GRID_LAYOUT_Y_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    width = inc * 4;

                if (data.start) {
                    idToCyNode = {};
                    data.vertices.forEach(function(v) {
                        idToCyNode[v.id] = cy.getElementById(toCyId(v));
                    });

                    // Sort existing nodes to end, except leave the first
                    // dragging vertex
                    vertices = data.vertices.sort(function(a,b) {
                        var cyA = idToCyNode[a.id], cyB = idToCyNode[b.id];
                        if (data.vertices[0].id === a.id) return -1;
                        if (data.vertices[0].id === b.id) return 1;
                        if (cyA.length && !cyB.length) return 1;
                        if (cyB.length && !cyA.length) return -1;

                        var titleA = F.vertex.title(a).toLowerCase(),
                            titleB = F.vertex.title(b).toLowerCase();

                        return titleA < titleB ? -1 : titleB < titleA ? 1 : 0;
                    });
                }

                vertices.forEach(function(vertex, i) {
                    var tempId = 'NEW-' + toCyId(vertex),
                        node = cy.getElementById(tempId);

                    if (node.length) {
                        node.renderedPosition(renderedPosition);
                    } else {
                        var classes = self.classesForVertex(vertex) + ' hover',
                            cyNode = idToCyNode[vertex.id];

                        if (cyNode.length) {
                            classes += ' existing';
                        }

                        var cyNodeData = {
                            group: 'nodes',
                            classes: classes,
                            data: {
                                id: tempId,
                            },
                            renderedPosition: renderedPosition,
                            selected: false
                        };
                        self.updateCyNodeData(cyNodeData.data, vertex);
                        cy.add(cyNodeData);
                    }

                    renderedPosition.x += inc;
                    if (renderedPosition.x > (start.x + width) || i === 0) {
                        renderedPosition.x = start.x;
                        renderedPosition.y += yinc;
                    }
                });
            });
        };

        this.onVerticesDropped = function(evt, data) {
            if (!this.isWorkspaceEditable) return;
            if (!this.$node.is(':visible')) return;
            this.cytoscapeReady(function(cy) {
                var self = this,
                    vertices = data.vertices,
                    position = data.dropPosition,
                    toFitTo = [],
                    toAnimateTo = [],
                    toRemove = [],
                    entityUpdates = [];

                vertices.forEach(function(vertex, i) {
                    var node = cy.getElementById('NEW-' + toCyId(vertex));
                    if (node.length === 0) return;
                    if (i === 0) position = node.position();
                    if (node.hasClass('existing')) {
                        var existingNode = cy.getElementById(node.id().replace(/^NEW-/, ''));
                        if (existingNode.length) toFitTo.push(existingNode);
                        toAnimateTo.push([node, existingNode]);
                        toFitTo.push(existingNode);
                    } else {
                        entityUpdates.push({
                            vertexId: vertex.id,
                            graphPosition: retina.pixelsToPoints(node.position())
                        });
                        node.ungrabify();
                        node.unselectify();
                        toRemove.push(node);
                    }
                });

                self.cyNodesToRemoveOnWorkspaceUpdated = cytoscape.Collection(cy, toRemove);

                if (toFitTo.length) {
                    cy.zoomOutToFit(cytoscape.Collection(cy, toFitTo), {
                        padding: this.paddingForZoomOut(),
                        callback: finished
                    });
                    animateToExisting(400);
                } else {
                    animateToExisting(0);
                    finished();
                }

                function animateToExisting(delay) {
                    toAnimateTo.forEach(function(args) {
                        self.animateFromToNode.apply(self, args.concat([delay]));
                    });
                }

                function finished() {
                    self.trigger('updateWorkspace', {
                        entityUpdates: entityUpdates
                    });
                    cy.container().focus();
                }
            });
        };

        this.onVerticesAdded = function(evt, data) {
            this.addVertices(data.vertices, data.options);
        };

        this.addVertices = function(vertices, opts) {
            var self = this,
                options = $.extend({ fit: false, animate: false }, opts),
                addedVertices = [],
                updatedVertices = [],
                dragging = $('.ui-draggable-dragging:not(.clone-vertex)'),
                isVisible = this.$node.closest('.visible').length === 1,
                cloned = null;

            if (dragging.length && isVisible) {
                cloned = dragging.clone()
                    .css({width: 'auto'})
                    .addClass('clone-vertex')
                    .insertAfter(dragging);
            }

            this.cytoscapeReady(function(cy) {
                var container = $(cy.container()),
                    currentNodes = cy.nodes(),
                    rawBoundingBox = currentNodes.boundingBox(),
                    availableSpaceBox = retina.pixelsToPoints({
                        x: isFinite(rawBoundingBox.x1) ? rawBoundingBox.x1 : 0,
                        y: isFinite(rawBoundingBox.y2) ? rawBoundingBox.y2 : 0,
                        w: isFinite(rawBoundingBox.w) ? rawBoundingBox.w : 0
                    }),
                    xInc = GRID_LAYOUT_X_INCREMENT,
                    yInc = GRID_LAYOUT_Y_INCREMENT,
                    nextAvailablePosition = _.pick(availableSpaceBox, 'x', 'y');

                if (options.animate) container.removeClass('animateinstart').addClass('animatein');

                if (options.fileDropPosition) {
                    var projectedPosition = cy.renderer().projectIntoViewport(
                        options.fileDropPosition.x,
                        options.fileDropPosition.y
                    );
                    nextAvailablePosition = retina.pixelsToPoints({
                        x: projectedPosition[0],
                        y: projectedPosition[1]
                    });
                } else {
                    nextAvailablePosition.y += yInc;
                }

                var maxWidth = Math.max(availableSpaceBox.w, xInc * 10),
                    startX = nextAvailablePosition.x,
                    vertexIds = _.pluck(vertices, 'id'),
                    existingNodes = currentNodes.filter(function(i, n) {
                        var nId = n.id();
                        if (/^NEW/.test(nId)) {
                            return -1;
                        }
                        return vertexIds.indexOf(fromCyId(nId)) >= 0;
                    }),
                    customLayout = $.Deferred();

                if (options.layout) {
                    require(['graph/layouts/' + options.layout.type], function(doLayout) {
                        customLayout.resolve(
                            doLayout(cy, currentNodes, rawBoundingBox, vertexIds, options.layout)
                        );
                    });
                } else customLayout.resolve({});

                customLayout.done(function(layoutPositions) {

                    var cyNodes = [];
                    vertices.forEach(function(vertex) {

                        var cyNodeData = {
                            group: 'nodes',
                            classes: self.classesForVertex(vertex),
                            data: {
                                id: toCyId(vertex),
                            },
                            grabbable: self.isWorkspaceEditable,
                            selected: false // TODO: check selected?
                        };
                        self.updateCyNodeData(cyNodeData.data, vertex);

                        var needsAdding = false,
                            needsUpdating = false;

                        if (self.workspaceVertices[vertex.id].graphPosition) {
                            cyNodeData.position = retina.pointsToPixels(
                                self.workspaceVertices[vertex.id].graphPosition
                            );
                        } else if (self.workspaceVertices[vertex.id].dropPosition) {
                            var offset = self.$node.offset();
                            cyNodeData.renderedPosition = retina.pointsToPixels({
                                x: self.workspaceVertices[vertex.id].dropPosition.x - offset.left,
                                y: self.workspaceVertices[vertex.id].dropPosition.y - offset.top
                            });
                            needsAdding = true;
                        } else if (layoutPositions[vertex.id]) {
                            cyNodeData.position = retina.pointsToPixels(layoutPositions[vertex.id]);
                            needsUpdating = true;
                        } else {

                            cyNodeData.position = retina.pointsToPixels(nextAvailablePosition);

                            nextAvailablePosition.x += xInc;
                            if ((nextAvailablePosition.x - startX) > maxWidth) {
                                nextAvailablePosition.y += yInc;
                                nextAvailablePosition.x = startX;
                            }

                            if (dragging.length === 0 || !isVisible) {
                                needsUpdating = true;
                            } else {
                                needsAdding = true;
                            }
                        }

                        if (needsAdding || needsUpdating) {
                            (needsAdding ? addedVertices : updatedVertices).push({
                                vertexId: vertex.id
                            });
                        }

                        cyNodes.push(cyNodeData);
                    });

                    var addedCyNodes = cy.add(cyNodes);
                    addedVertices.concat(updatedVertices).forEach(function(v) {
                        v.graphPosition = retina.pixelsToPoints(cy.getElementById(toCyId(v)).position());
                        self.workspaceVertices[v.vertexId] = v;
                    });

                    if (options.fit && cy.nodes().length) {

                        _.defer(self.fit.bind(self));

                    } else if (isVisible && options.addingVerticesRelatedTo) {
                        var relatedToCyNode = cy.getElementById(self.toCyId(options.addingVerticesRelatedTo));
                        if (relatedToCyNode.length) {
                            var nodes = addedCyNodes.add(relatedToCyNode);

                            _.defer(function() {
                                cy.zoomOutToFit(nodes, {
                                    padding: self.paddingForZoomOut()
                                });
                            })
                        }
                    }

                    if (options.animate) {
                        if (cy.nodes().length) {
                            _.delay(function again() {
                                container.on(TRANSITION_END, function(e) {
                                    container.off(TRANSITION_END);
                                    container.removeClass('animatein animatestart');
                                });
                                container.addClass('animateinstart');

                            }, 250);
                        } else container.removeClass('animatein animateinstart');
                    }

                    if (cloned && !(existingNodes.length && cloned && cloned.length)) {
                        cloned.remove();
                    }

                    if (updatedVertices.length || addedVertices.length) {
                        self.trigger('updateWorkspace', {
                            entityUpdates: updatedVertices.concat(addedVertices)
                        });
                    }

                    self.hideLoading();

                    self.setWorkspaceDirty();
                });
            });
        };

        this.classesForVertex = function(vertex) {
            var cls = [];

            if (F.vertex.imageIsFromConcept(vertex) === false) {
                cls.push('hasCustomGlyph');
            }
            if (~['video', 'image'].indexOf(F.vertex.concept(vertex).displayType)) {
                cls.push(F.vertex.concept(vertex).displayType);
            }

            return cls.join(' ');
        };

        this.updateCyNodeData = function(data, vertex) {
            var truncatedTitle = F.string.truncate(F.vertex.title(vertex), 3),
                merged = data;

            merged.truncatedTitle = truncatedTitle;
            merged.conceptType = F.vertex.prop(vertex, 'conceptType');
            merged.imageSrc = F.vertex.image(vertex);

            return merged;
        };

        this.onVerticesDeleted = function(event, data) {
            this.cytoscapeReady(function(cy) {

                if (data.vertices.length) {
                    cy.$(
                        data.vertices.map(function(v) {
                        return '#' + toCyId(v);
                    }).join(',')).remove();

                    this.setWorkspaceDirty();
                    this.updateVertexSelections(cy);
                }
            });
        };

        this.onObjectsSelected = function(evt, data) {
            if ($(evt.target).is('.graph-pane')) {
                return;
            }

            this.cytoscapeReady(function(cy) {
                this.ignoreCySelectionEvents = true;

                cy.$(':selected').unselect();

                var vertices = data.vertices,
                    edges = data.edges,
                    cyNodes;
                if (vertices.length || edges.length) {
                    cyNodes = cy.$(
                        vertices.concat(edges).map(function(v) {
                            return '#' + toCyId(v);
                        }).join(',')
                    ).select();
                }

                setTimeout(function() {
                    this.ignoreCySelectionEvents = false;
                }.bind(this), SELECTION_THROTTLE * 1.5);

                if (cyNodes && cyNodes.length) {
                    this.nodesToFitAfterGraphPadding = cyNodes;
                }
            });
        };

        this.onVerticesUpdated = function(evt, data) {
            var self = this;
            this.cytoscapeReady(function(cy) {
                data.vertices
                    .forEach(function(updatedVertex) {
                        var cyNode = cy.nodes().filter('#' + toCyId(updatedVertex));
                        if (cyNode.length) {
                            if (updatedVertex.workspace.graphPosition) {
                                cyNode.position(retina.pointsToPixels(updatedVertex.workspace.graphPosition));
                            }

                            var newData = self.updateCyNodeData(cyNode.data(), updatedVertex);
                            cyNode.data(newData);
                            if (cyNode._private.classes) {
                                cyNode._private.classes.length = 0;
                            }
                            cyNode.addClass(self.classesForVertex(updatedVertex));
                        }
                    });
            });

            this.setWorkspaceDirty();
        };

        this.animateFromToNode = function(cyFromNode, cyToNode, delay) {
            var self = this,
                cy = cyFromNode.cy();

            if (cyToNode && cyToNode.length) {
                cyFromNode
                    .css('opacity', 1.0)
                    .stop(true)
                    .delay(delay)
                    .animate(
                        {
                            position: cyToNode.position()
                        },
                        {
                            duration: 500,
                            easing: 'easeOutBack',
                            complete: function() {
                                cyFromNode.remove();
                            }
                        }
                    );
            } else {
                cyFromNode.remove();
            }
        };

        this.onContextMenuExportWorkspace = function(exporterId) {
            var exporter = WorkspaceExporters.exportersById[exporterId],
                $node = this.$node,
                workspaceId = this.previousWorkspace;

            if (exporter) {
                require(['util/popovers/exportWorkspace/exportWorkspace'], function(ExportWorkspace) {
                    ExportWorkspace.attachTo($node, {
                        exporter: exporter,
                        workspaceId: workspaceId,
                        anchorTo: {
                            page: {
                                x: window.lastMousePositionX,
                                y: window.lastMousePositionY
                            }
                        }
                    });
                });
            }
        };

        this.onContextMenuZoom = function(level) {
            this.cytoscapeReady(function(cy) {
                cy.zoom(level);
            });
        };

        this.onContextMenuDeleteEdge = function() {
            var menu = this.select('edgeContextMenuSelector'),
                edge = menu.data('edge').vertex;

            this.trigger('deleteEdges', { edges: [edge] });
        };

        this.onEdgesDeleted = function(event, data) {
            this.cytoscapeReady(function(cy) {
                var edge = cy.getElementById(this.toCyId(data.edgeId));
                edge.remove();
            });
        };

        this.onContextMenuFitToWindow = function() {
            this.fit();
        };

        this.onContextMenuCreateVertex = function() {
            var menu = this.select('contextMenuSelector');
            this.createVertex(menu.offset());
        }

        this.updateEdgeOptions = function(cy) {
            cy.renderer().hideEdgesOnViewport = cy.edges().length > SHOW_EDGES_ON_ZOOM_THRESHOLD;
        };

        this.onDevicePixelRatioChanged = function() {
            this.cytoscapeReady(function(cy) {
                cy.renderer().updatePixelRatio();
                this.fit(cy);
            });
        };

        this.fit = function(cy, nodes) {
            var self = this;

            if (cy) {
                _fit(cy);
            } else {
                this.cytoscapeReady(_fit);
            }

            function _fit(cy) {
                if (cy.elements().size() === 0){
                    cy.reset();
                } else if (self.graphPadding) {
                    // Temporarily adjust max zoom
                    // prevents extreme closeup when one vertex
                    var maxZoom = cy._private.maxZoom;
                    cy._private.maxZoom *= 0.5;
                    cy.panningEnabled(true).zoomingEnabled(true).boxSelectionEnabled(true);
                    cy.fit(nodes, $.extend({}, self.graphPadding));
                    cy._private.maxZoom = maxZoom;
                }
            }
        };

        this.verticesForGraphIds = function(cy, vertexIds, type) {
            var selector = vertexIds.map(function(vId) {
                return '#' + toCyId(vId);
            }).join(',');

            return cy[type || 'nodes'](selector);
        };

        this.onFocusVertices = function(e, data) {
            this.cytoscapeReady(function(cy) {
                var vertexIds = data.vertexIds;
                this.hoverDelay = _.delay(function() {
                    var nodes = this.verticesForGraphIds(cy, vertexIds, 'nodes')
                            .css('borderWidth', 0)
                            .addClass('focus'),
                        edges = this.verticesForGraphIds(cy, vertexIds, 'edges')
                            .css('width', 1.5 * retina.devicePixelRatio)
                            .addClass('focus');

                    function animate(elements, options) {
                        if (!elements.hasClass('focus')) {
                            elements.css(options.reset);
                            return;
                        }

                        if (_.isUndefined(options.animateValue)) {
                            options.animateValue = options.end;
                        }

                        var css = {
                                // Opacity         1 -> .75
                                // borderWidth start -> end
                                opacity: 1 - (
                                    (options.animateValue - options.start) /
                                    (options.end - options.start) * 0.25
                                )
                            },
                            elementsLength = elements.length;

                        css[options.animateProperty] = options.animateValue;

                        elements.animate({
                            css: css
                        }, {
                            duration: 1200,
                            easing: 'easeInOutCirc',
                            complete: function() {
                                if (--elementsLength === 0) {
                                    options.animateValue = options.animateValue === options.start ?
                                        options.end : options.start;
                                    animate(elements, options)
                                }
                            }
                        });
                    }

                    if (nodes.length) {
                        animate(nodes, {
                            start: 1 * retina.devicePixelRatio,
                            end: 30 * retina.devicePixelRatio,
                            animateProperty: 'borderWidth',
                            reset: {
                                borderWidth: 0,
                                opacity: 1
                            }
                        });
                    }
                    if (edges.length) {
                        animate(edges, {
                            start: 1.5 * retina.devicePixelRatio,
                            end: 4.5 * retina.devicePixelRatio,
                            animateProperty: 'width',
                            reset: {
                                width: 1.5 * retina.devicePixelRatio,
                                opacity: 1
                            }
                        });
                    }
                }.bind(this), HOVER_FOCUS_DELAY_SECONDS * 1000);
            });
        };

        this.onDefocusVertices = function(e, data) {
            clearTimeout(this.hoverDelay);
            this.cytoscapeReady(function(cy) {
                cy.elements('.focus').removeClass('focus').stop(true, true);
            });
        };

        this.onFocusPaths = function(e, data) {
            this.cytoscapeReady(function(cy) {
                var paths = data.paths,
                    sourceId = data.sourceId,
                    targetId = data.targetId;

                cy.$('.path-edge').removeClass('path-edge path-hidden-verts');
                cy.$('.path-temp').remove();

                paths.forEach(function(path, i) {
                    var vertexIds = _.chain(path)
                                .filter(function(v) {
                                    return v.id !== sourceId && v.id !== targetId;
                                })
                                .pluck('id')
                                .value(),
                        end = colorjs('#0088cc').shiftHue(i * (360 / paths.length)).toCSSHex(),
                        lastNode = cy.getElementById(toCyId(sourceId)),
                        count = 0,
                        existingOrNewEdgeBetween = function(node1, node2, count) {
                            var edge = node1.edgesWith(node2);
                            if (!edge.length || edge.removed() || edge.hasClass('path-edge')) {
                                edge = cy.add({
                                    group: 'edges',
                                    classes: 'path-temp' + (count ? ' path-hidden-verts' : ''),
                                    id: node1.id() + '-' + node2.id() + 'path=' + i,
                                    data: {
                                        source: node1.id(),
                                        target: node2.id(),
                                        label: count === 0 ? '' :
                                            i18n('graph.path.edge.label.' + (
                                                count === 1 ? 'one' : 'some'
                                            ), F.number.pretty(count))
                                    }
                                });
                            }
                            edge.addClass('path-edge');
                            edge.data('pathColor', end);
                            return edge;
                        };

                    vertexIds.forEach(function(vId, i) {
                        var thisNode = cy.getElementById(toCyId(vId));
                        if (thisNode.length && !thisNode.removed()) {
                            existingOrNewEdgeBetween(lastNode, thisNode, count);
                            lastNode = thisNode;
                            count = 0;
                        } else count++;
                    });

                    existingOrNewEdgeBetween(lastNode, cy.getElementById(toCyId(targetId)), count);
                });
            });
        };

        this.onDefocusPaths = function(e, data) {
            this.cytoscapeReady(function(cy) {
                cy.$('.path-edge').removeClass('path-edge path-hidden-verts');
                cy.$('.path-temp').remove();
            });
        };

        this.onGraphPaddingUpdated = function(e, data) {
            var self = this,
                border = 20;

            this.graphPaddingRight = data.padding.r;

            var padding = $.extend({}, data.padding);

            padding.r += this.select('graphToolsSelector').outerWidth(true) || 65;
            padding.l += border;
            padding.t += border;
            padding.b += border;
            this.graphPadding = padding;

            if (this.nodesToFitAfterGraphPadding) {
                this.cytoscapeReady().done(function(cy) {
                    cy.zoomOutToFit(self.nodesToFitAfterGraphPadding, {
                        padding: self.paddingForZoomOut()
                    });
                    self.nodesToFitAfterGraphPadding = null;
                });
            }
        };

        this.onContextMenuLayout = function(layout, opts) {
            var self = this,
                options = $.extend({onlySelected: false}, opts);

            this.cytoscapeReady(function(cy) {

                var unselected;
                if (options.onlySelected) {
                    unselected = cy.nodes().filter(':unselected');
                    unselected.lock();
                }

                var opts = $.extend({
                    name: layout,
                    fit: false,
                    stop: function() {
                        if (unselected) {
                            unselected.unlock();
                        }
                        var updates = $.map(cy.nodes(), function(vertex) {
                            return {
                                id: fromCyId(vertex.id()),
                                workspace: {
                                    graphPosition: retina.pixelsToPoints(vertex.position())
                                }
                            };
                        });
                        self.trigger(document, 'updateVertices', { vertices: updates });
                        self.fit(cy);
                    }
                }, LAYOUT_OPTIONS[layout] || {});

                cy.layout(opts);
            });
        };

        this.graphTap = throttle('selection', SELECTION_THROTTLE, function(event) {
            this.trigger('defocusPaths');

            if (event.cyTarget === event.cy) {
                this.trigger('selectObjects');
            }
        });

        this.graphContextTap = function(event) {
            var self = this,
                menu;

            if (event.cyTarget == event.cy){
                menu = this.select ('contextMenuSelector');
                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.select('edgeContextMenuSelector').blur().parent().removeClass('open');
                this.trigger('closeVertexMenu');
            } else if (event.cyTarget.group ('edges') == 'edges') {

                if (Privileges.canEDIT) {
                    menu = this.select ('edgeContextMenuSelector');
                    var edgeData = event.cyTarget.data();
                    if (!(/^public$/i).test(edgeData.vertex.diffType)) {
                        menu.data('edge', edgeData);
                        if (event.cy.nodes().filter(':selected').length > 1) {
                            return false;
                        }
                    } else {
                        menu = null;
                    }
                }

                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.select('contextMenuSelector').blur().parent().removeClass('open');
            } else {
                this.select('edgeContextMenuSelector').blur().parent().removeClass('open');
                this.select('contextMenuSelector').blur().parent().removeClass('open');

                var originalEvent = event.originalEvent;
                this.trigger(this.select('cytoscapeContainerSelector')[0], 'showVertexContextMenu', {
                    vertexId: fromCyId(event.cyTarget.id()),
                    position: {
                        x: originalEvent.pageX,
                        y: originalEvent.pageY
                    }
                });

                return;
            }

            if (menu) {
                // Show/Hide the layout selection menu item
                if (event.cy.nodes().filter(':selected').length) {
                    menu.find('.layout-multi').show();
                } else {
                    menu.find('.layout-multi').hide();
                }

                if (WorkspaceExporters.exporters.length) {
                    var $exporters = menu.find('.exporters');

                    if ($exporters.length === 0) {
                        $exporters = $('<li class="dropdown-submenu"><a>' +
                          i18n('graph.contextmenu.export_workspace') +
                          '</a>' +
                          '<ul class="dropdown-menu exporters"></ul></li>'
                         ).appendTo(menu).find('ul');
                    }

                    $exporters.empty();
                    WorkspaceExporters.exporters.forEach(function(exporter) {
                        $exporters.append(
                            $('<li><a href="#"></a></li>')
                                .find('a')
                                    .text(exporter.menuItem)
                                    .attr('data-func', 'exportWorkspace')
                                    .attr('data-args', JSON.stringify([exporter._identifier]))
                                .end()
                        );
                    });
                }

                this.toggleMenu({positionUsingEvent: event}, menu);
            }
        };

        this.graphSelect = throttle('selection', SELECTION_THROTTLE, function(event) {
            if (this.ignoreCySelectionEvents) return;
            this.updateVertexSelections(event.cy);
        });

        this.graphUnselect = throttle('selection', SELECTION_THROTTLE, function(event) {
            if (this.ignoreCySelectionEvents) return;

            var self = this,
                selection = event.cy.elements().filter(':selected');

            if (!selection.length) {
                self.trigger('selectObjects');
            }
        });

        this.updateVertexSelections = function(cy) {
            var self = this,
                nodes = cy.nodes().filter(':selected').not('.temp'),
                edges = cy.edges().filter(':selected').not('.temp'),
                vertexIds = [];

            nodes.each(function(index, cyNode) {
                if (!cyNode.hasClass('temp') && !cyNode.hasClass('path-edge')) {
                    vertexIds.push(fromCyId(cyNode.id()));
                }
            });

            this.dataRequest('vertex', 'store', { vertexIds: vertexIds })
                .done(function(vertices) {

                    edges.each(function(index, cyEdge) {
                        if (!cyEdge.hasClass('temp') && !cyEdge.hasClass('path-edge')) {
                            var vertex = cyEdge.data('vertex');
                            vertices.push(vertex)
                        }
                    });

                    // Only allow one edge selected
                    if (nodes.length === 0 && edges.length > 1) {
                        vertices = [vertices[0]];
                    }
                    if (vertices.length > 0){
                        self.trigger('selectObjects', { vertices: vertices });
                    } else {
                        self.trigger('selectObjects');
                    }
                })
        };

        this.graphGrab = function(event) {
            var self = this;
            this.trigger('defocusPaths');
            this.cytoscapeReady(function(cy) {
                var vertices = event.cyTarget.selected() ? cy.nodes().filter(':selected').not('.temp') : event.cyTarget;
                this.grabbedVertices = vertices.each(function() {
                    var p = retina.pixelsToPoints(this.position());
                    this.data('originalPosition', { x: p.x, y: p.y });
                    this.data('freed', false);
                });
            });
        };

        this.graphFree = function(event) {
            if (!this.isWorkspaceEditable) return;
            var self = this,
                dup = true, // CY is sending multiple "free" events, prevent that...
                vertices = this.grabbedVertices;

            if (!vertices || vertices.length === 0) return;

            var cy = vertices[0].cy(),
                updateData = {},
                verticesMoved = [];

            vertices.each(function(i, vertex) {
                var p = retina.pixelsToPoints(vertex.position()),
                    cyId = vertex.id(),
                    pCopy = {
                        x: Math.round(p.x),
                        y: Math.round(p.y)
                    };

                if (!vertex.data('freed')) {
                    dup = false;
                }

                // Rounding can cause vertex to jump 1/2 pixel
                // Jump immediately instead of after save
                vertex.position(retina.pointsToPixels(pCopy));

                updateData[cyId] = {
                    targetPosition: pCopy,
                    freed: true
                };
                verticesMoved.push({
                    vertexId: fromCyId(cyId),
                    graphPosition: pCopy
                });
            });

            if (dup) {
                return;
            }

            cy.batchData(updateData);

            // If the user didn't drag more than a few pixels, select the
            // object, it could be an accidental mouse move
            var target = vertices[0],
                p = target.data('targetPosition'),
                originalPosition = target.data('originalPosition'),
                dx = p.x - originalPosition.x,
                dy = p.y - originalPosition.y,
                distance = Math.sqrt(dx * dx + dy * dy);

            if (distance < 5) {
                event.cyTarget.select();
            }

            this.trigger('updateWorkspace', {
                entityUpdates: verticesMoved
            });
            this.setWorkspaceDirty();
        };

        this.graphMouseOver = function(event) {
            var self = this,
                cyNode = event.cyTarget;

            clearTimeout(this.mouseoverTimeout);

            if (cyNode !== event.cy && cyNode.group() === 'nodes') {
                this.mouseoverTimeout = _.delay(function() {
                    var nId = cyNode.id();
                    if (/^NEW/.test(nId)) {
                        return;
                    }

                    self.dataRequest('vertex', 'store', { vertexIds: fromCyId(nId) })
                        .done(function(vertex) {
                            truncatedTitle = cyNode.data('truncatedTitle');

                            if (vertex) {
                                cyNode.data('previousTruncated', truncatedTitle);
                                cyNode.data('truncatedTitle', F.vertex.title(vertex));
                            }
                        })
                }, 500);
            }
        };

        this.graphMouseOut = function(event) {
            clearTimeout(this.mouseoverTimeout);
            if (event.cyTarget !== event.cy) {
                event.cyTarget.data('truncatedTitle', event.cyTarget.data('previousTruncated'));
            }
        };

        this.setWorkspaceDirty = function() {
            this.checkEmptyGraph();
        };

        this.checkEmptyGraph = function() {
            this.cytoscapeReady(function(cy) {
                var noVertices = cy.nodes().length === 0;

                this.select('emptyGraphSelector').toggle(noVertices);
                cy.panningEnabled(!noVertices)
                    .zoomingEnabled(!noVertices)
                    .boxSelectionEnabled(!noVertices);

                this.select('graphToolsSelector').toggle(!noVertices);
                if (noVertices) {
                    cy.reset();
                }

                this.updateEdgeOptions(cy);
            });
        };

        this.resetGraph = function() {
            this.cytoscapeReady(function(cy) {
                cy.elements().remove();
                this.setWorkspaceDirty();
            });
        };

        this.hideLoading = function() {
            var loading = this.$node.find('.loading-graph');
            if (loading.length) {
                loading.on(TRANSITION_END, function(e) {
                    loading.remove();
                });
                loading.addClass('hidden');
                _.delay(function() {
                    loading.remove();
                }, 2000);
            }
        };

        this.getNodesByVertexIds = function(cy, list, optionalVertexIdAccessor) {
            if (list.length === 0) {
                return cy.collection();
            }

            return cy.$(
                list.map(function(obj) {
                    return '#' + toCyId(optionalVertexIdAccessor ? obj[optionalVertexIdAccessor] : obj);
                }).join(',')
            );
        };

        this.onWorkspaceUpdated = function(event, data) {
            if (this.previousWorkspace === data.workspace.workspaceId) {
                this.isWorkspaceEditable = data.workspace.editable;
                this.cytoscapeReady(function(cy) {
                    var self = this,
                        allNodes = cy.nodes();

                    allNodes[data.workspace.editable ? 'grabify' : 'ungrabify']();

                    data.entityUpdates.forEach(function(entityUpdate) {
                        var cyNode = cy.getElementById(toCyId(entityUpdate.vertexId));
                        if (cyNode.length && !cyNode.grabbed()) {
                            cyNode.position(retina.pointsToPixels(entityUpdate.graphPosition));
                        }
                        self.workspaceVertices[entityUpdate.vertexId] = entityUpdate;
                    });

                    this.getNodesByVertexIds(cy, data.entityDeletes).remove();
                    if (data.newVertices) {
                        if (this.cyNodesToRemoveOnWorkspaceUpdated) {
                            this.cyNodesToRemoveOnWorkspaceUpdated.remove();
                            this.cyNodesToRemoveOnWorkspaceUpdated = null;
                        }
                        this.addVertices(data.newVertices)
                    }
                    this.setWorkspaceDirty();
                    this.updateVertexSelections(cy);
                });
            }
        }

        this.onWorkspaceLoaded = function(evt, workspace) {
            this.resetGraph();
            this.isWorkspaceEditable = workspace.editable;
            this.workspaceVertices = workspace.vertices;
            if (workspace.data.vertices.length) {
                var newWorkspace = !this.previousWorkspace || this.previousWorkspace != workspace.workspaceId;
                this.addVertices(workspace.data.vertices, {
                    fit: newWorkspace,
                    animate: false
                });
            } else {
                this.hideLoading();
                this.checkEmptyGraph();
            }

            this.previousWorkspace = workspace.workspaceId;
        };

        this.onEdgesLoaded = function(evt, relationshipData) {
            this.cytoscapeReady(function(cy) {
                if (relationshipData.edges) {
                    var relationshipEdges = [];
                    relationshipData.edges.forEach(function(relationship) {
                        var sourceNode = cy.getElementById(toCyId(relationship.from)),
                            destNode = cy.getElementById(toCyId(relationship.to));

                        if (sourceNode.length && destNode.length) {
                            relationshipEdges.push ({
                                group: 'edges',
                                data: {
                                    id: toCyId(relationship.id),
                                    source: sourceNode.id(),
                                    target: destNode.id(),
                                    vertex: {
                                        id: relationship.id,
                                        diffType: relationship.diffType,
                                        properties: {
                                            'http://lumify.io#conceptType': 'relationship',
                                            source: relationship.from,
                                            target: relationship.to,
                                            relationshipType: relationship.relationshipType
                                        }
                                    }
                                }
                            });
                        }
                    });
                    // Hide edges when zooming if more than threshold
                    if (relationshipEdges.length) {
                        cy.edges().remove();
                        cy.add(relationshipEdges);
                    }

                    this.updateEdgeOptions(cy);
                }
            });
        };

        this.onMenubarToggleDisplay = function(e, data) {
            if (data.name === 'graph' && this.$node.is(':visible')) {
                this.cytoscapeReady(function(cy) {
                    cy.renderer().notify({type: 'viewport'});

                    if (this.fitOnMenubarToggle) {
                        this.fit(cy);
                        this.fitOnMenubarToggle = false;
                    }
                });
            }
        };

        this.onRegisterForPositionChanges = function(event, data) {
            var self = this,
                anchorTo = data && data.anchorTo;

            if (!anchorTo || (!anchorTo.vertexId && !anchorTo.page)) {
                return console.error('Registering for position events requires a vertexId');
            }

            //this.onUnregisterForPositionChanges();

            this.cytoscapeReady().done(function(cy) {

                event.stopPropagation();

                var cyNode = anchorTo.vertexId && cy.getElementById(toCyId(anchorTo.vertexId)),
                    offset = self.$node.offset(),
                    cyPosition = anchorTo.page && cy.renderer().projectIntoViewport(
                        anchorTo.page.x + offset.left,
                        anchorTo.page.y + offset.top
                    );

                if (!self.onViewportChangesForPositionChanges) {
                    self.onViewportChangesForPositionChanges = function() {
                        var position;

                        if (anchorTo.vertexId) {
                            var positionInNode = retina.pixelsToPoints(cyNode.renderedPosition());

                            position = {
                                x: positionInNode.x + offset.left,
                                y: positionInNode.y + offset.top,
                            };

                        } else if (anchorTo.page) {
                            position = retina.pixelsToPoints({
                                x: cyPosition[0] * cy.zoom() + cy.pan().x,
                                y: cyPosition[1] * cy.zoom() + cy.pan().y
                            });
                        }

                        self.trigger(event.target, 'positionChanged', { position: position });
                    };
                }

                cy.on('pan zoom position', self.onViewportChangesForPositionChanges);
                self.onViewportChangesForPositionChanges();
            })
        };

        this.onUnregisterForPositionChanges = function(event, data) {
            var self = this;
            this.cytoscapeReady().done(function(cy) {
                if (self.onViewportChangesForPositionChanges) {
                    cy.off('pan zoom position', self.onViewportChangesForPositionChanges);
                    self.onViewportChangesForPositionChanges = null;
                }
            });
        };

        this.paddingForZoomOut = function() {
            return _.tap(_.extend({}, this.graphPadding), function(p) {
                var extra = 50;

                p.t += extra;
                p.b += extra;
                p.l += extra;
                // Right has the zoom controls overlap with it
            });
        }

        this.onShowPanel = function() {
            this.cytoscapeReady().done(function(cy) {
                cy.startAnimationLoop();
                cy.renderer().notify({type: 'viewport'});
            });
        };

        this.onHidePanel = function() {
            this.cytoscapeReady().done(function(cy) {
                cy.stopAnimationLoop();
            });
        };

        this.onShowMenu = function(event, data) {
            var self = this;

            this.cytoscapeReady()
                .done(function(cy) {
                    var offset = self.$node.offset(),
                        r = cy.renderer(),
                        pos = r.projectIntoViewport(
                            data.pageX,// - offset.left,
                            data.pageY// - offset.top
                        ),
                        near = r.findNearestElement(pos[0], pos[1], true);

                    self.graphContextTap({
                        cyTarget: near || cy,
                        cy: cy,
                        originalEvent: _.pick(data, 'pageX', 'pageY')
                    })
                });
        };

        this.onHideMenu = function(event) {
            this.trigger(document, 'closeVertexMenu');
            this.select('contextMenuSelector').blur().parent().removeClass('open');
            this.select('edgeContextMenuSelector').blur().parent().removeClass('open');
        };

        this.createVertex = function(offset) {
            var self = this;
            if (Privileges.canEDIT) {
                require(['util/popovers/createVertex/createVertex'], function(CreateVertex) {
                    CreateVertex.attachTo(self.$node, {
                        anchorTo: {
                            page: {
                                x: offset.left,
                                y: offset.top
                            }
                        }
                    });
                });
            }
        };

        this.onCreateVertex = function(e, data) {
            this.createVertex({
                left: data && data.pageX || 0,
                top: data && data.pageY || 0
            })
        };

        this.after('teardown', function() {
            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            this.setupAsyncQueue('cytoscape');

            this.$node.html(loadingTemplate({}));

            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'verticesHovering', this.onVerticesHovering);
            this.on(document, 'verticesHoveringEnded', this.onVerticesHoveringEnded);
            this.on(document, 'verticesAdded', this.onVerticesAdded);
            this.on(document, 'verticesDropped', this.onVerticesDropped);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'edgesLoaded', this.onEdgesLoaded);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'devicePixelRatioChanged', this.onDevicePixelRatioChanged);
            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);
            this.on(document, 'focusVertices', this.onFocusVertices);
            this.on(document, 'defocusVertices', this.onDefocusVertices);
            this.on(document, 'focusPaths', this.onFocusPaths);
            this.on(document, 'defocusPaths', this.onDefocusPaths);
            this.on(document, 'edgesDeleted', this.onEdgesDeleted);

            this.on('registerForPositionChanges', this.onRegisterForPositionChanges);
            this.on('unregisterForPositionChanges', this.onUnregisterForPositionChanges);
            this.on('showPanel', this.onShowPanel);
            this.on('hidePanel', this.onHidePanel);
            this.on('showMenu', this.onShowMenu);
            this.on('hideMenu', this.onHideMenu);
            this.on('createVertex', this.onCreateVertex);
            this.on('contextmenu', function(e) {
                e.preventDefault();
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: i18n('graph.help.scope'),
                shortcuts: {
                    '-': { fire: 'zoomOut', desc: i18n('graph.help.zoom_out') },
                    '=': { fire: 'zoomIn', desc: i18n('graph.help.zoom_in') },
                    'alt-f': { fire: 'fit', desc: i18n('graph.help.fit') },
                    'alt-n': { fire: 'createVertex', desc: i18n('graph.help.create_vertex') }
                }
            });

            if (self.attr.vertices && self.attr.vertices.length) {
                this.select('emptyGraphSelector').hide();
                this.addVertices(self.attr.vertices);
            }

            this.dataRequest('ontology', 'concepts')
                .done(function(concepts) {
                    var templateData = {
                        firstLevelConcepts: concepts.entityConcept.children || [],
                        pathHopOptions: ['2','3','4']
                    };

                    // TODO: make context menus work better
                    self.$node.append(template(templateData)).find('.shortcut').each(function() {
                        var $this = $(this), command = $this.text();
                        $this.text(F.string.shortcut($this.text()));
                    });

                    self.bindContextMenuClickEvent();

                    Controls.attachTo(self.select('graphToolsSelector'));

                    stylesheet(function(style) {
                        self.initializeGraph(style);
                    });
                });
        });

        this.initializeGraph = function(style) {
            var self = this;

            cytoscape('renderer', 'lumify', Renderer);
            cytoscape({
                showOverlay: false,
                minZoom: 1 / 4,
                maxZoom: 4,
                container: this.select('cytoscapeContainerSelector').css({height: '100%'})[0],
                renderer: {
                    name: 'lumify'
                },
                style: style,

                ready: function() {
                    var cy = this,
                        container = cy.container(),
                        options = cy.options();

                    cy.on({
                        tap: self.graphTap.bind(self),
                        select: self.graphSelect.bind(self),
                        unselect: self.graphUnselect.bind(self),
                        grab: self.graphGrab.bind(self),
                        free: self.graphFree.bind(self),
                        mouseover: self.graphMouseOver.bind(self),
                        mouseout: self.graphMouseOut.bind(self)
                    });

                    self.on('pan', function(e, data) {
                        e.stopPropagation();
                        cy.panBy(data.pan);
                    });
                    self.on('fit', function(e) {
                        e.stopPropagation();
                        self.fit(cy);
                    });

                    var zoomFactor = 0.05,
                        zoom = function(factor) {
                            var pan = cy.pan(),
                                zoom = cy.zoom(),
                                w = self.$node.width(),
                                h = self.$node.height(),
                                pos = cy.renderer().projectIntoViewport(w / 2 + self.$node.offset().left, h / 2),
                                unpos = [pos[0] * zoom + pan.x, pos[1] * zoom + pan.y];

                            cy.zoom({
                                level: cy.zoom() + factor,
                                position: { x: unpos[0], y: unpos[1] }
                            })
                        };

                    self.on('zoomIn', function(e) {
                        zoom(zoomFactor);
                    });
                    self.on('zoomOut', function(e) {
                        zoom(zoomFactor * -1);
                    });
                },
                done: function() {
                    self.cytoscapeMarkReady(this);

                    if (self.$node.is('.visible')) {
                        setTimeout(function() {
                            self.fit();
                        }, 100);
                    } else self.fitOnMenubarToggle = true;
                }
            });
        };
    }

});
