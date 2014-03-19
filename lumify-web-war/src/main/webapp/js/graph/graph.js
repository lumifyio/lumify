

define([
    'flight/lib/component',
    'data',
    'cytoscape',
    './renderer',
    './stylesheet',
    './contextmenu/withGraphContextMenuItems',
    './withControlDrag',
    'tpl!./graph',
    'tpl!./loading',
    'util/controls',
    'util/throttle',
    'util/formatters',
    'service/vertex',
    'service/ontology',
    'service/config',
    'util/retina',
    'util/withContextMenu',
    'util/withAsyncQueue',
    'colorjs'
], function(
    defineComponent,
    appData,
    cytoscape,
    Renderer,
    stylesheet,
    withGraphContextMenuItems,
    withControlDrag,
    template,
    loadingTemplate,
    Controls,
    throttle,
    formatters,
    VertexService,
    OntologyService,
    ConfigService,
    retina,
    withContextMenu,
    withAsyncQueue,
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

    return defineComponent(Graph, withAsyncQueue, withContextMenu, withGraphContextMenuItems, withControlDrag);

    function Graph() {
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();
        this.configService = new ConfigService();

        var LAYOUT_OPTIONS = {
                // Customize layout options
                random: { padding: 10 },
                arbor: { friction: 0.6, repulsion: 5000 * retina.devicePixelRatio, targetFps: 60, stiffness: 300 }
            },
            vertexIdIndex = 0,
            vertexIdMap = {},
            cyIdMap = {},
            vertexId = function(v) {
                var vId = _.isString(v) ? v : v.id,
                    cyId = vertexIdMap[vId];
                        
                if (!cyId) {
                    cyId = vertexIdMap[vId] = ('vid' + (vertexIdIndex++));
                }

                cyIdMap[cyId] = vId;
                return cyId;
            };

        this.vertexId = vertexId;
        this.cyIdMap = cyIdMap;
        this.vertexIdMap = vertexIdMap;

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
                return this.trigger('displayInformation', { message: 'Workspace is read only' })
            }
            this.cytoscapeReady(function(cy) {
                var self = this,
                    offset = this.$node.offset(),
                    renderedPosition = retina.pointsToPixels({
                        x: data.position.x - offset.left,
                        y: data.position.y - offset.top
                    }),
                    start = {
                        x:renderedPosition.x,
                        y:renderedPosition.y
                    },
                    inc = GRID_LAYOUT_X_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    yinc = GRID_LAYOUT_Y_INCREMENT * cy.zoom() * retina.devicePixelRatio,
                    width = inc * 4;

                if (data.start) {
                    idToCyNode = {};
                    data.vertices.forEach(function(v) {
                        idToCyNode[v.id] = cy.getElementById(vertexId(v));
                    });

                    // Sort existing nodes to end, except leave the first
                    // dragging vertex
                    vertices = data.vertices.sort(function(a,b) {
                        var cyA = idToCyNode[a.id], cyB = idToCyNode[b.id];
                        if (data.vertices[0].id === a.id) return -1;
                        if (data.vertices[0].id === b.id) return 1;
                        if (cyA.length && !cyB.length) return 1;
                        if (cyB.length && !cyA.length) return -1;

                        var titleA = a.properties.title.value.toLowerCase(),
                            titleB = b.properties.title.value.toLowerCase();

                        return titleA < titleB ? -1 : titleB < titleA ? 1 : 0;
                    });
                }

                vertices.forEach(function(vertex, i) {
                    var tempId = 'NEW-' + vertexId(vertex),
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
                    toFitTo = [],
                    toAnimateTo = [],
                    toRemove = [],
                    toAdd = [],
                    position;

                vertices.forEach(function(vertex, i) {
                    var node = cy.getElementById('NEW-' + vertexId(vertex));
                    if (node.length === 0) return;
                    if (i === 0) position = node.position();
                    if (node.hasClass('existing')) {
                        var existingNode = cy.getElementById(node.id().replace(/^NEW-/, ''));
                        if (existingNode.length) toFitTo.push(existingNode);
                        toAnimateTo.push([node, existingNode]);
                        toFitTo.push(existingNode);
                    } else {
                        vertex.workspace.graphPosition = retina.pixelsToPoints(node.position());
                        toAdd.push(vertex);
                        toRemove.push(node);
                    }
                });

                if (toFitTo.length) {
                    cy.zoomOutToFit(cytoscape.Collection(cy, toFitTo), $.extend({}, this.graphPadding), position, finished);
                    animateToExisting(200);
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
                    cytoscape.Collection(cy, toRemove).remove();
                    self.trigger('addVertices', { vertices:toAdd });
                    cy.container().focus();
                }
            });
        };

        this.onVerticesAdded = function(evt, data) {
            this.addVertices(data.vertices, data.options);
        };

        this.addVertices = function(vertices, opts) {
            var options = $.extend({ fit:false, animate:false }, opts),
                addedVertices = [],
                updatedVertices = [],
                self = this;

            if (self.addingRelatedVertices) {
                _.defer(function() {
                    self.trigger('displayInformation', { message: 'Related Entities Added'});
                });
                self.addingRelatedVertices = false;
            }

            var dragging = $('.ui-draggable-dragging:not(.clone-vertex)'),
                cloned = null;
            if (dragging.length && this.$node.closest('.visible').length === 1) {
                cloned = dragging.clone()
                    .css({width:'auto'})
                    .addClass('clone-vertex')
                    .insertAfter(dragging);
            }

            this.cytoscapeReady(function(cy) {
                var container = $(cy.container()),
                    currentNodes = cy.nodes(),
                    boundingBox = currentNodes.boundingBox(),
                    validBox = isFinite(boundingBox.x1),
                    zoom = cy.zoom(),
                    xInc = GRID_LAYOUT_X_INCREMENT,
                    yInc = GRID_LAYOUT_Y_INCREMENT,
                    nextAvailablePosition = retina.pixelsToPoints({ 
                        x: validBox ? (boundingBox.x1/* + xInc*/) : 0,
                        y: validBox ? (boundingBox.y2/* + yInc*/) : 0
                    });

                if (options.animate) container.removeClass('animateinstart').addClass('animatein');

                nextAvailablePosition.y += yInc;

                var maxWidth = validBox ? retina.pixelsToPoints({ x:boundingBox.w, y:boundingBox.h}).x : 0,
                    startX = nextAvailablePosition.x;

                maxWidth = Math.max(maxWidth, xInc * 10);

                var vertexIds = _.pluck(vertices, 'id'),
                    existingNodes = currentNodes.filter(function(i, n) { return vertexIds.indexOf(cyIdMap[n.id()]) >= 0; }), 
                    customLayout = $.Deferred();

                if (options.layout) {
                    require(['graph/layouts/' + options.layout.type], function(doLayout) {
                        customLayout.resolve(
                            doLayout(cy, currentNodes, boundingBox, vertexIds, cyIdMap, options.layout)
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
                                id: vertexId(vertex),
                            },
                            grabbable: self.isWorkspaceEditable,
                            selected: !!vertex.workspace.selected
                        };
                        self.updateCyNodeData(cyNodeData.data, vertex);

                        var needsAdding = false,
                            needsUpdating = false;

                        if (vertex.workspace.graphPosition) {
                            cyNodeData.position = retina.pointsToPixels(vertex.workspace.graphPosition);
                        } else if (vertex.workspace.dropPosition) {
                            var offset = self.$node.offset();
                            cyNodeData.renderedPosition = retina.pointsToPixels({
                                x: vertex.workspace.dropPosition.x - offset.left,
                                y: vertex.workspace.dropPosition.y - offset.top
                            });
                            needsAdding = true;
                        } else if (layoutPositions[vertex.id]) {
                            cyNodeData.position = retina.pointsToPixels(layoutPositions[vertex.id]);
                            needsUpdating = true;
                        } else {

                            cyNodeData.position = retina.pointsToPixels(nextAvailablePosition);

                            nextAvailablePosition.x += xInc;
                            if((nextAvailablePosition.x - startX) > maxWidth) {
                                nextAvailablePosition.y += yInc;
                                nextAvailablePosition.x = startX;
                            }

                            if (dragging.length === 0) {
                                needsUpdating = true;
                            } else {
                                needsAdding = true;
                            }
                        }


                        if (needsAdding || needsUpdating) {
                            (needsAdding ? addedVertices : updatedVertices).push({
                                id: vertex.id,
                                workspace: {
                                }
                            });
                        }

                        cyNodes.push(cyNodeData);
                    });

                    cy.add(cyNodes);
                    addedVertices.concat(updatedVertices).forEach(function(v) {
                        v.workspace.graphPosition = cy.getElementById(vertexId(v)).position();
                    });


                    if (options.fit && cy.nodes().length) {
                        _.defer(self.fit.bind(self));
                    }
                    if (options.animate) {
                        if (cy.nodes().length) {
                            _.delay(function again() {
                                container.on('transitionend webkitTransitionEnd MSTransitionEnd oTransitionEnd', function(e) {
                                    container.removeClass('animatein animatestart');
                                });
                                container.addClass('animateinstart');

                            }, 250);
                        } else container.removeClass('animatein animateinstart');
                    }

                    if (existingNodes.length && cloned && cloned.length) {
                        // Animate to something
                    } else if (cloned) cloned.remove();

                    if (updatedVertices.length) {
                        self.trigger(document, 'updateVertices', { vertices:updatedVertices });
                    } else if (addedVertices.length) {
                        container.focus();
                        self.trigger(document, 'addVertices', { vertices:addedVertices });
                    }

                    self.hideLoading();

                    self.setWorkspaceDirty();
                });
            });
        };

        this.classesForVertex = function(vertex) {
            if (vertex.properties._glyphIcon) return 'hasCustomGlyph';

            return '';
        };

        this.updateCyNodeData = function (data, vertex) {
            var truncatedTitle = vertex.properties.title.value || vertex.properties.title || "No title available";

            if (truncatedTitle.length > MAX_TITLE_LENGTH) {
                truncatedTitle = $.trim(truncatedTitle.substring(0, MAX_TITLE_LENGTH)) + "...";
            }

            var merged = $.extend(data, _.pick(vertex.properties, '_rowKey', 'http://lumify.io#conceptType', '_glyphIcon', 'title'));
            merged.truncatedTitle = truncatedTitle;
            merged.imageSrc = vertex.imageSrc;

            return merged;
        };

        this.onVerticesDeleted = function(event, data) {
            this.cytoscapeReady(function(cy) {

                if (data.vertices.length) {
                    cy.$( 
                        data.vertices.map(function(v) { return '#' + vertexId(v); }).join(',')
                    ).remove();

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
                    edges = data.edges;
                if (vertices.length || edges.length) {
                    cy.$( 
                        vertices.concat(edges).map(function(v) {
                            return '#' + vertexId(v);
                        }).join(',')
                    ).select();
                }

                setTimeout(function() {
                    this.ignoreCySelectionEvents = false;
                }.bind(this), SELECTION_THROTTLE * 1.5);
            });
        };

        this.onVerticesUpdated = function(evt, data) {
            var self = this;
            this.cytoscapeReady(function(cy) {
                data.vertices
                    .forEach(function(updatedVertex) {
                        var cyNode = cy.nodes().filter('#' + vertexId(updatedVertex));
                        if (cyNode.length) {
                            if (updatedVertex.workspace.graphPosition) {
                                cyNode.position( retina.pointsToPixels(updatedVertex.workspace.graphPosition) );
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
                            duration: 700,
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

        this.onContextMenuSearchFor = function () {
            var menu = this.select('vertexContextMenuSelector');
            this.trigger(document, 'searchByEntity', { query : menu.data('title')});
        };

        this.onContextMenuSearchRelated = function () {
            var menu = this.select('vertexContextMenuSelector');
            this.trigger(document, 'searchByRelatedEntity', { vertexId : menu.data('currentVertexGraphVertexId')});
        };

        this.onContextMenuZoom = function(level) {
            this.cytoscapeReady(function(cy) {
                cy.zoom(level);
            });
        };

        this.onContextMenuLoadRelatedItems = function () {
            var data = this.setupLoadRelatedItems();
            this.onLoadRelatedSelected(data);
        };

        this.onContextMenuLoadRelatedItemsOfConcept = function(conceptId) {
            var data = this.setupLoadRelatedItems();
            data.limitParentConceptId = conceptId;
            this.onLoadRelatedSelected(data);
        };

        this.setupLoadRelatedItems = function() {
            var menu = this.select('vertexContextMenuSelector');
            var currentVertexRK = menu.data('currentVertexRowKey');
            var graphVertexId = menu.data('currentVertexGraphVertexId');
            var position = {x: menu.data ('currentVertexPositionX'), y: menu.data ('currentVertexPositionY')};
            var currentVertexOriginalPosition = retina.pixelsToPoints(position);
            var data = {
                _rowKey: currentVertexRK,
                graphVertexId: graphVertexId,
                originalPosition: currentVertexOriginalPosition
            };
            return data;
        };

        this.onContextMenuDeleteEdge = function () {
            var menu = this.select('edgeContextMenuSelector'),
                edge = {
                    id: menu.data('edge').id,
                    properties: menu.data('edge')
                };

            this.trigger('deleteEdges', { edges:[edge] });
        };

        this.onEdgesDeleted = function (event, data) {
            this.cytoscapeReady(function (cy) {
                cy.remove('#' + this.vertexId(data.edgeId));
                this.updateEdgeOptions(cy);
            });
        };

        this.onContextMenuRemoveItem = function (){
            var menu = this.select('vertexContextMenuSelector'),
                vertex = {
                    id: menu.data('currentVertexGraphVertexId')
                };
            this.trigger(document,'deleteVertices', {vertices:[vertex] });
        };

        this.onContextMenuFitToWindow = function() {
            this.fit();
        };

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
                if( cy.elements().size() === 0 ){
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
                return '#' + vertexId(vId); 
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

                paths.forEach(function(path, i) {
                    var vertexIds = _.chain(path)
                                .filter(function(v) { return v.id !== sourceId && v.id !== targetId; })
                                .pluck('id')
                                .value(),
                        end = colorjs('#0088cc').shiftHue(i * (360 / paths.length)).toCSSHex(),
                        lastNode = cy.getElementById(vertexId(sourceId)),
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
                                        label: count ? 
                                            formatters.string.plural(count, 'vertex', 'vertices') : ''
                                    }
                                });
                            }
                            edge.addClass('path-edge');
                            edge.data('pathColor', end);
                            return edge;
                        };

                    vertexIds.forEach(function(vId, i) {
                        var thisNode = cy.getElementById(vertexId(vId));
                        if (thisNode.length && !thisNode.removed()) {
                            existingOrNewEdgeBetween(lastNode, thisNode, count);
                            lastNode = thisNode;
                            count = 0;
                        } else count++;
                    });

                    existingOrNewEdgeBetween(lastNode, cy.getElementById(vertexId(targetId)), count);
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
            var border = 20;
            this.graphPaddingRight = data.padding.r;

            var padding = $.extend({}, data.padding);

            padding.r += this.select('graphToolsSelector').outerWidth(true) || 65;
            padding.l += border;
            padding.t += border;
            padding.b += border;
            this.graphPadding = padding;
        };

        this.onContextMenuLayout = function(layout, opts) {
            var self = this;
            var options = $.extend({onlySelected:false}, opts);
            this.cytoscapeReady(function(cy) {

                var unselected;
                if (options.onlySelected) {
                    unselected = cy.nodes().filter(':unselected');
                    unselected.lock();
                }

                var opts = $.extend({
                    name:layout,
                    fit: false,
                    stop: function() {
                        if (unselected) {
                            unselected.unlock();
                        }
                        var updates = $.map(cy.nodes(), function(vertex) {
                            return {
                                id: cyIdMap[vertex.id()],
                                workspace: {
                                    graphPosition: retina.pixelsToPoints(vertex.position())
                                }
                            };
                        });
                        self.trigger(document, 'updateVertices', { vertices:updates });
                    }
                }, LAYOUT_OPTIONS[layout] || {});

                cy.layout(opts);
            });
        };

        this.graphTap = throttle('selection', SELECTION_THROTTLE, function(event) {
            if (event.cyTarget === event.cy) {
                this.trigger('selectObjects');
            }
        });

        this.graphContextTap = function(event) {
            var menu;

            if (event.cyTarget == event.cy){
                menu = this.select ('contextMenuSelector');
                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.select('edgeContextMenuSelector').blur().parent().removeClass('open');
            } else if (event.cyTarget.group ('edges') == 'edges') {
                menu = this.select ('edgeContextMenuSelector');
                menu.data("edge", event.cyTarget.data());
                if (event.cy.nodes().filter(':selected').length > 1) {
                    return false;
                }
                this.select('vertexContextMenuSelector').blur().parent().removeClass('open');
                this.select('contextMenuSelector').blur().parent().removeClass('open');
            } else {
                var title = event.cyTarget.data('title').value;
                if (title.length > MAX_TITLE_LENGTH) {
                    title = $.trim(title.substring(0, MAX_TITLE_LENGTH)) + "...";
                }

                this.$node.find('a').each(function() {
                    var $this = $(this),
                        template = $this.data('template');

                    if (template) {
                        var children = $this.find('*').remove();
                        $this.text(
                            _.template(template)({title:title})
                        ).append(children);
                    }
                });

                menu = this.select ('vertexContextMenuSelector');
                menu.data("currentVertexRowKey",event.cyTarget.data('_rowKey'));
                menu.data("currentVertexGraphVertexId", cyIdMap[event.cyTarget.id()]);
                menu.data("currentVertexPositionX", event.cyTarget.position ('x'));
                menu.data("currentVertexPositionY", event.cyTarget.position ('y'));
                menu.data("title", event.cyTarget.data('title').value);
                this.select('contextMenuSelector').blur().parent().removeClass('open');
                this.select('edgeContextMenuSelector').blur().parent().removeClass('open');
            }

            // Show/Hide the layout selection menu item
            if (event.cy.nodes().filter(':selected').length) {
                menu.find('.layout-multi').show();
            } else {
                menu.find('.layout-multi').hide();
            }

            this.toggleMenu({positionUsingEvent:event}, menu);
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
            var nodes = cy.nodes().filter(':selected').not('.temp'),
                edges = cy.edges().filter(':selected').not('.temp'),
                vertices = [];

            nodes.each(function(index, cyNode) {
                if (!cyNode.hasClass('temp') && !cyNode.hasClass('path-edge')) {
                    vertices.push(appData.vertex(cyIdMap[cyNode.id()]));
                }
            });

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
                this.trigger('selectObjects', { vertices:vertices });
            } else {
                this.trigger('selectObjects');
            }
        };

        this.graphGrab = function(event) {
            var self = this;
            this.cytoscapeReady(function(cy) {
                var vertices = event.cyTarget.selected() ? cy.nodes().filter(':selected').not('.temp') : event.cyTarget;
                this.grabbedVertices = vertices.each(function() {
                    var p = retina.pixelsToPoints(this.position());
                    this.data('originalPosition', { x:p.x, y:p.y });
                    this.data('freed', false );
                });
            });
        };

        this.graphFree = function(event) {
            if (!this.isWorkspaceEditable) return;
            var self = this;

            // CY is sending multiple "free" events, prevent that...
            var dup = true,
                vertices = this.grabbedVertices;

            if (!vertices) return;
            vertices.each(function(i, e) {
                var p = retina.pixelsToPoints(this.position());
                if ( !e.data('freed') ) {
                    dup = false;
                }
                e.data('targetPosition', {x:p.x, y:p.y});
                e.data('freed', true);
            });

            if (dup) {
                return;
            }

            // If the user didn't drag more than a few pixels, select the
            // object, it could be an accidental mouse move
            var target = event.cyTarget, 
                p = retina.pixelsToPoints(target.position()),
                originalPosition = target.data('originalPosition'),
                dx = p.x - originalPosition.x,
                dy = p.y - originalPosition.y,
                distance = Math.sqrt(dx * dx + dy * dy);

            if (distance === 0) return;
            if (distance < 5) {
                if (!event.originalEvent.shiftKey) {
                    event.cy.$(':selected').unselect();
                }
                target.select();
            }

            // Cache these positions since data attr could be overidden
            // then submit to undo manager
            var originalPositions = [], targetPositions = [];
            vertices.each(function(i, e) {
                originalPositions.push( e.data('originalPosition') );
                targetPositions.push( e.data('targetPosition') );
            });

            var graphMovedVerticesData = {
                vertices: $.map(vertices, function(vertex) {
                    return {
                        id: cyIdMap[vertex.id()],
                        workspace: {
                            graphPosition: vertex.data('targetPosition')
                        }
                    };
                })
            };
            self.trigger(document, 'updateVertices', graphMovedVerticesData);

            this.setWorkspaceDirty();
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
                loading.on('transitionend webkitTransitionEnd MSTransitionEnd oTransitionEnd', function(e) {
                    loading.remove();
                });
                loading.addClass('hidden');
                setTimeout(function() { loading.remove(); }, 2000);
            }
        };

        this.onWorkspaceLoaded = function(evt, workspace) {
            this.resetGraph();
            this.isWorkspaceEditable = workspace.isEditable;
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

        this.onRelationshipsLoaded = function(evt, relationshipData) {
            this.cytoscapeReady(function(cy) {
                if (relationshipData.relationships) {
                    var relationshipEdges = [];
                    relationshipData.relationships.forEach(function(relationship) {
                        var sourceNode = cy.getElementById(vertexId(relationship.from)),
                            destNode = cy.getElementById(vertexId(relationship.to));

                        if (sourceNode.length && destNode.length) {
                            relationshipEdges.push ({
                                group: "edges",
                                data: {
                                    id: vertexId(relationship.id),
                                    source: sourceNode.id(),
                                    target: destNode.id(),
                                    vertex: {
                                        id: relationship.id,
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


        this.onLoadRelatedSelected = function(data) {

            if ($.isArray(data) && data.length){
                data = data[0];
            }

            var self = this,
                vId = data.graphVertexId,
                LoadingPopover,
                req,
                cancelHandler = function() {
                    if (req) {
                        req.abort();
                    }
                    self.off('popovercancel');
                },
                timeout = _.delay(function() {
                    self.trigger('hideInformation');
                    require(['graph/popovers/loadingPopover'], function(LP) {
                        LoadingPopover = LP;
                        LoadingPopover.teardownAll();
                        self.cytoscapeReady().done(function(cy) {
                            LoadingPopover.attachTo(self.$node, {
                                cy: cy,
                                cyNode: cy.getElementById(vertexId(vId)),
                                message: 'Loading Related...'
                            });
                        });
                    });
                }, 1000);

            this.trigger('displayInformation', { message: 'Loading Related...', dismissDuration:1000 });

            this.on('popovercancel', cancelHandler);

            $.when(
                this.configService.getProperties(),
                (req = this.vertexService.getRelatedVertices(data)),
                this.cytoscapeReady()
            ).always(function() {
                clearTimeout(timeout);
                if (LoadingPopover) {
                    LoadingPopover.teardownAll();
                }
                self.trigger('hideInformation');
                self.off('popovercancel');
            }).fail(function() {
                self.trigger('displayInformation', {
                    message: 'Error Loading Related Items',
                    dismiss: 'click',
                    dismissDuration: 5000
                });
            }).done(function(config, verticesResponse, cy) {

                var vertices = verticesResponse[0].vertices,
                    count = vertices.length,
                    forceSearch = count > config['vertex.loadRelatedMaxForceSearch'],
                    promptBeforeAdding = count > config['vertex.loadRelatedMaxBeforePrompt'];
                
                if (count > 0 && (forceSearch || promptBeforeAdding)) {
                    require(['graph/popovers/loadRelatedPopover'], function(LoadRelatedPopover) {
                        LoadRelatedPopover.teardownAll();
                        LoadRelatedPopover.attachTo(self.$node, {
                            addToWorkspaceEvent: 'addRelatedToWorkspace',
                            forceSearch: forceSearch,
                            count: count,
                            cy: cy,
                            cyNode: cy.getElementById(vertexId(vId)),
                            relatedToVertexId: vId,
                            vertices: vertices
                        });
                    });
                } else self.trigger('addRelatedToWorkspace', { vertices:vertices });
            });
        };

        this.onAddRelatedToWorkspace = function(event, data) {
            var vertices = data.vertices;
            this.cytoscapeReady(function(cy) {
                cy.filter(':selected').unselect();
                cy.container().focus();
                vertices.forEach(function(vertex, index) {
                    vertex.workspace = {
                        selected: true
                    };
                });

                this.addingRelatedVertices = true;
                this.trigger('addVertices', { vertices:vertices });
                this.trigger('selectObjects', { vertices:vertices })
            });
        };

        this.onLoadRelatedItems = function() {
            var vertexIds = appData.selectedVertexIds;
            if (vertexIds.length === 1) {
                this.onLoadRelatedSelected({ graphVertexId: vertexIds[0] });
            }
        };

        this.onSearchTitle = function() {
            this.cytoscapeReady(function(cy) {
                var selected = cy.nodes().filter(':selected');
                if (selected.length) {
                    this.trigger(document, 'searchByEntity', { query : selected.data('title').value});
                }
            });
        }

        this.onSearchRelated = function() {
            this.cytoscapeReady(function(cy) {
                var selected = cy.nodes().filter(':selected');
                if (selected.length) {
                    this.trigger(document, 'searchByRelatedEntity', { vertexId : cyIdMap[selected.id()] });
                }
            });
        }

        this.onMenubarToggleDisplay = function(e, data) {
            if (data.name === 'graph' && this.$node.is(':visible')) {
                this.cytoscapeReady(function(cy) {
                    cy.renderer().notify({type:'viewport'});

                    if (this.fitOnMenubarToggle) {
                        this.fit(cy);
                        this.fitOnMenubarToggle = false;
                    }
                });
            }
        };

        this.after('teardown', function() {
            this.$node.empty();
        });

        this.after('initialize', function() {
            var self = this;

            this.setupAsyncQueue('cytoscape');

            this.$node.html(loadingTemplate({}));

            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'verticesHovering', this.onVerticesHovering);
            this.on(document, 'verticesHoveringEnded', this.onVerticesHoveringEnded);
            this.on(document, 'verticesAdded', this.onVerticesAdded);
            this.on(document, 'verticesDropped', this.onVerticesDropped);
            this.on(document, 'verticesDeleted', this.onVerticesDeleted);
            this.on(document, 'verticesUpdated', this.onVerticesUpdated);
            this.on(document, 'objectsSelected', this.onObjectsSelected);
            this.on(document, 'relationshipsLoaded', this.onRelationshipsLoaded);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'devicePixelRatioChanged', this.onDevicePixelRatioChanged);
            this.on(document, 'menubarToggleDisplay', this.onMenubarToggleDisplay);
            this.on(document, 'focusVertices', this.onFocusVertices);
            this.on(document, 'defocusVertices', this.onDefocusVertices);
            this.on(document, 'focusPaths', this.onFocusPaths);
            this.on(document, 'defocusPaths', this.onDefocusPaths);
            this.on(document, 'edgesDeleted', this.onEdgesDeleted);

            this.on('loadRelatedItems', this.onLoadRelatedItems);
            this.on('searchTitle', this.onSearchTitle);
            this.on('searchRelated', this.onSearchRelated)
            this.on('addRelatedToWorkspace', this.onAddRelatedToWorkspace);

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: 'Graph',
                shortcuts: {
                    '-': { fire:'zoomOut', desc:'Zoom out' },
                    '=': { fire:'zoomIn', desc:'Zoom in' },
                    'alt-f': { fire:'fit', desc:'Fit all objects on screen' },
                    'alt-r': { fire:'loadRelatedItems', desc:'Add related items to workspace' },
                    'alt-t': { fire:'searchTitle', desc:'Search for selected title' },
                    'alt-s': { fire:'searchRelated', desc:'Search vertices related to selected' },
                }
            });

            if (self.attr.vertices && self.attr.vertices.length) {
                this.select('emptyGraphSelector').hide();
                this.addVertices(self.attr.vertices);
            }

            this.ontologyService.concepts().done(function(concepts) {
                var templateData = {
                    firstLevelConcepts: concepts.entityConcept.children || [],
                    pathHopOptions: ["2","3","4"]
                };

                // TODO: make context menus work better
                self.$node.append(template(templateData)).find('.shortcut').each(function() {
                    var $this = $(this), command = $this.text();
                    $this.text(formatters.string.shortcut($this.text()));
                });

                Controls.attachTo(self.select('graphToolsSelector'));

                stylesheet(function(style) {
                    self.initializeGraph(style);
                });
            });
        });

        this.initializeGraph = function(style) {
            var self = this;

            cytoscape("renderer", "lumify", Renderer);
            cytoscape({
                showOverlay: false,
                minZoom: 1 / 4,
                maxZoom: 4,
                container: this.select('cytoscapeContainerSelector').css({height:'100%'})[0],
                renderer: {
                    name: 'lumify'
                },
                style: style,

                ready: function(){
                    var cy = this;

                    var container = cy.container(),
                        options = cy.options();

                    cy.on({
                        tap: self.graphTap.bind(self),
                        cxttap: self.graphContextTap.bind(self),
                        select: self.graphSelect.bind(self),
                        unselect: self.graphUnselect.bind(self),
                        grab: self.graphGrab.bind(self),
                        free: self.graphFree.bind(self)
                    });

                    self.bindContextMenuClickEvent();

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
                                pos = cy.renderer().projectIntoViewport(w/2 + self.$node.offset().left, h/2),
                                unpos = [pos[0] * zoom + pan.x, pos[1] * zoom + pan.y];

                            cy.zoom({
                                level: cy.zoom() + factor,
                                position: { x: unpos[0], y: unpos[1] }
                            })
                        };

                    self.on('zoomIn', function(e) { zoom(zoomFactor); });
                    self.on('zoomOut', function(e) { zoom(-zoomFactor); });
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

