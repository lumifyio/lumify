define([
    'data',
    'util/retina',
    'util/formatters',
    'service/vertex'
], function(appData, retina, formatters, VertexService) {
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
            hops,
            connectionType;

        if (!this.vertexService) {
            this.vertexService = new VertexService();
        }

        this.defaultAttrs({
            dialogSelector: '.connect-dialog',
            findPathSelector: '.connect-dialog .find-path',
            findPathButtonSelector: '.connect-dialog .find-path-form button',
            findPathHopsButtonSelector: '.connect-dialog .popover-title .dropdown-menu a',
            createConnectionSelector: '.connect-dialog .create-connection',
            createConnectionButtonSelector: '.connect-dialog .create-connection-form button'
        });

        this.after('initialize', function() {
            var self = this,
                lockedCyTarget;

            this.mouseDragHandler = self.onControlDragMouseMove.bind(this);
            this.onViewportChanges = _.throttle(this.onViewportChanges.bind(this), 100);

            this.on(document, 'controlKey', function() { controlKeyPressed = true; });
            this.on(document, 'controlKeyUp', function() { controlKeyPressed = false; });

            this.on('click', {
                findPathSelector: this.onFindPath,
                findPathButtonSelector: this.onFindPathButton,
                createConnectionSelector: this.onCreateConnection,
                createConnectionButtonSelector: this.onCreateConnectionButton
            });

            this.on('startVertexConnection', this.onStartVertexConnection);
            this.on('endVertexConnection', this.onEndVertexConnection);
            this.on('finishedVertexConnection', this.onFinishedVertexConnection);
            this.on('selectObjects', this.onSelectObjects);

            this.cytoscapeReady(function(cy) {
                cy.on({
                    tap: function(event) {
                        if (state === STATE_CONNECTED) {
                            this.trigger('finishedVertexConnection');
                        }
                    },
                    tapstart: function(event) {
                        if (state > STATE_NONE) return;

                        if (controlKeyPressed && event.cyTarget !== cy) {
                            self.trigger('startVertexConnection', {
                                sourceId: event.cyTarget.id()
                            });
                        }
                    },
                    grab: function(event) {

                        if (controlKeyPressed) {
                            if (state > STATE_NONE) {
                                self.trigger('finishedVertexConnection');
                            }
                            lockedCyTarget = event.cyTarget;
                            lockedCyTarget.lock();
                        }
                    },
                    free: function() {
                        if (state === STATE_STARTED) {
                            self.trigger('endVertexConnection', {
                                edgeId: currentEdgeId
                            });
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

        this.showForm = function(formName) {
            var form = this.$node.find('.popover-content .' + formName),
                dialog = form.closest('.connect-dialog');

            dialog.find('.form-button').hide();
            dialog.find('.popover-title > div').hide();
            dialog.find('.popover-title .' + formName).show();
            return form;
        };

        this.onCreateConnection = function() {
            this.cytoscapeReady(function(cy) {
                var self = this,
                    form = this.showForm('create-connection-form'),
                    select = form.find('select'),
                    title = form.closest('.popover').find('.popover-title .title'),
                    button = form.find('button');
                
                select.html('<option>Loading...</option>');
                title.text(cy.getElementById(currentSourceId).data('title')).closest('.popover-title').show();
                button.text('Connect').attr('disabled', true);
                form.show();

                this.positionDialog();

                button.focus();

                this.getRelationshipLabels(
                    cy.getElementById(currentSourceId),
                    cy.getElementById(currentTargetId)
                ).done(function(relationships) {

                    if (relationships.length) {
                        select.html(
                            relationships.map(function(d){
                                return '<option value="' + d.title + '">' + d.displayName + '</option>';
                            }).join('')
                        ).siblings('button').removeAttr('disabled');
                    } else {
                        select.html('<option>No valid relationships</option>');
                    }

                    self.positionDialog();
                });
            });
        };

        this.onCreateConnectionButton = function(e) {
            if (!currentEdgeId) return;

            var $target = $(e.target);

            $target.text('Connecting...').attr('disabled', true);

            this.cytoscapeReady(function(cy) {
                var self = this,
                    parameters = {
                        sourceGraphVertexId: currentSourceId,
                        destGraphVertexId: currentTargetId,
                        predicateLabel: $target.siblings('select').val()
                    };

                this.relationshipService.createRelationship(parameters)
                    .done(function(data) {
                        self.on(document, 'relationshipsLoaded', function loaded() {
                            self.trigger('finishedVertexConnection');
                            self.off(document, 'relationshipsLoaded', loaded);
                        });
                        // TODO: should we send an expected relationship so
                        // data.js will continue checking until it's eventually
                        // consistent?
                        self.trigger('refreshRelationships');
                    });
            });
        };

        this.onFindPath = function() {
            this.cytoscapeReady(function(cy) {
                var self = this,
                    src = currentSourceId,
                    dest = currentTargetId,
                    form = this.showForm('find-path-form'),
                    popoverTitle = form.closest('.popover'),
                    title = popoverTitle.find('.popover-title .title'),
                    text = form.find('span'),
                    button = form.find('button');

                this.trigger('defocusPaths');
                
                text.text('Loading...');
                button.hide().text('Add Vertices').attr('disabled', true);
                form.show();

                this.select('findPathHopsButtonSelector').off('click').on('click', this.onFindPathHopsButton.bind(this));

                this.positionDialog();

                button.focus();

                this.findPath(src, dest).done(function(result){

                    var paths = result.paths,
                        vertices = result.uniqueVertices,
                        verticesNotSourceDest = vertices.filter(function(v) {
                            return v.id !== src && v.id !== dest;
                        }),
                        notInWorkspace = vertices.filter(function(v) { 
                            return !appData.workspaceVertices[v.id]; 
                        }),
                        pathsFoundText = formatters.string.plural(paths.length, 'path') + ' found';

                    if (paths.length) {
                        if (notInWorkspace.length) {
                            var vertexText = formatters.string.plural(notInWorkspace.length, 'vertex', 'vertices'),
                                suffix = notInWorkspace.length === 1 ? ' isn\'t' : ' aren\'t';
                            text.text(vertexText + suffix + ' already in workspace');
                            button.text('Add ' + vertexText).removeAttr('disabled').show();

                            var index, map = {};
                            for (var i = 0; i < notInWorkspace.length; i++) {
                                path_loop: for (var j = 0; j < paths.length; j++) {
                                    for (var x = 0; x < paths[j].length; x++) {
                                        if (paths[j][x].id === notInWorkspace[i].id) {
                                            map[notInWorkspace[i].id] = {
                                                sourceId: paths[j][x-1].id,
                                                targetId: paths[j][x+1].id
                                            };
                                            break path_loop;
                                        }
                                    }
                                }
                            }

                            self.verticesToAdd = notInWorkspace;
                            self.verticesToAddLayoutMap = map;
                        } else {
                            text.text('all vertices are already added to workspace');
                        }

                        cy.$('.temp').remove();
                        self.trigger('focusPaths', { paths:paths, sourceId:src, targetId:dest });
                    } else text.text('Path search using ' + formatters.string.plural(hops, 'hop'));


                    title.text(pathsFoundText).closest('.popover-title').show();
                    self.positionDialog();
                });
            });
        };

        this.onFindPathButton = function(e) {
            var vertices = this.verticesToAdd;

            this.trigger('finishedVertexConnection');
            this.trigger('addVertices', { 
                vertices: vertices,
                options: {
                    layout: {
                        type: 'path',
                        map: this.verticesToAddLayoutMap
                    }
                }
            });
            this.trigger('selectObjects', { vertices:vertices });
        };

        this.onFindPathHopsButton = function(e) {
            var $target = $(e.target),
                newHops = $target.data('hops');

            if ($target.closest('.disabled').length) return;

            var list = $target.closest('ul')
            
            list.siblings('.dropdown-toggle').html($target.data('displayText') + ' <span class="caret"/>');
            list.find('.disabled').removeClass('disabled');
            $target.closest('li').addClass('disabled');

            hops = newHops;
            this.onFindPath();
        };

        this.onFinishedVertexConnection = function(event) {
            state = STATE_NONE;

            this.cytoscapeReady(function(cy) {
                cy.$('.temp').remove();
                cy.$('.controlDragSelection').removeClass('controlDragSelection');
                cy.off('pan zoom position', this.onViewportChanges);
                currentEdgeId = null;
                currentSourceId = null;
                currentTargetId = null;
                this.select('dialogSelector').hide();
                this.ignoreCySelectionEvents = false;
                this.trigger('defocusPaths');
            });
        };

        this.showDialog = function(edge) {
            this.cytoscapeReady(function(cy) {
                var self = this,
                    targetId = edge.data('target');

                this.currentTargetNode = cy.getElementById(targetId);
                this.onViewportChanges();
                
                var dialog = this.$node.find('.connect-dialog');
                dialog.parent('div').css({ position: 'absolute' });
                var popoverTitle = dialog.find('.popover-title').hide();

                popoverTitle.find('.dropdown-toggle').html('Shortest <span class="caret"/>');
                popoverTitle.find('.disabled').removeClass('disabled');
                popoverTitle.find('.dropdown-menu li:first-child').addClass('disabled');

                if (connectionType) {
                    dialog.find('.form').hide();
                    dialog.find('.form-button').hide();
                    this.positionDialog();

                    self['on' + connectionType]();
                } else {
                    dialog.find('.form-button').show();
                    dialog.find('.form').hide();
                    this.positionDialog();
                }

                cy.on('pan zoom position', this.onViewportChanges);
            });
        };

        this.onViewportChanges = function() {
            this.dialogPosition = retina.pixelsToPoints(this.currentTargetNode.renderedPosition());
            this.dialogPosition.y -= this.currentTargetNode.height() / 2 * this.currentTargetNode.cy().zoom();
            this.positionDialog();
        };

        this.positionDialog = function() {
            var dialog = this.select('dialogSelector'),
                width = dialog.outerWidth(true),
                height = dialog.outerHeight(true),
                proposed = {
                    left: Math.max(0, Math.min(this.$node.width() - width, this.dialogPosition.x - (width / 2))),
                    top: Math.max(0, Math.min(this.$node.height() - height, this.dialogPosition.y - height))
                };

            dialog.parent('div').css(proposed);
            dialog.show();
        }

        this.onStartVertexConnection = function(event, data) {
            state = STATE_STARTED;
            connectionType = data.connectionType;
            hops = data.hops || 1;

            this.ignoreCySelectionEvents = true;

            this.cytoscapeReady(function(cy) {
                startControlDragTarget = cy.getElementById(data.sourceId);
                cy.nodes().lock();
                cy.on('mousemove', this.mouseDragHandler);
            });
        };

        this.onEndVertexConnection = function(event, data) {
            state = STATE_CONNECTED;

            this.cytoscapeReady(function(cy) {
                cy.off('mousemove', this.mouseDragHandler);
                cy.nodes().unlock();
                startControlDragTarget = null;

                var edge = currentEdgeId && cy.getElementById(currentEdgeId);
                if (edge && !cy.getElementById(edge.data('target')).hasClass('temp')) {
                    this.showDialog(edge);
                } else {
                    this.trigger('finishedVertexConnection');
                }
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
                        group: "edges",
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


        this.findPath = function(source, dest) {
            var parameters = {
                sourceGraphVertexId: source,
                destGraphVertexId: dest,
                depth: 5,
                hops: hops
            };

            return this.vertexService.findPath(parameters)
                        .then(function (data) {
                            var vertices = [], added = {};
                            data.paths.forEach(function (path) {
                                path.forEach(function (vertex) {
                                    if (!added[vertex.id]) {
                                        vertices.push(vertex);
                                        added[vertex.id] = true;
                                    }
                                });
                            });

                            return {
                                paths: data.paths,
                                uniqueVertices: vertices
                            };
                        });
        };
        
        this.getRelationshipLabels = function (source, dest) {
            var self = this,
                sourceConceptTypeId = source.data('_conceptType');
                destConceptTypeId = dest.data('_conceptType');

            return $.when(
                this.ontologyService.conceptToConceptRelationships(sourceConceptTypeId, destConceptTypeId),
                this.ontologyService.relationships()
            ).then(function(conceptToConceptResponse, ontologyRelationships) {
                var results = conceptToConceptResponse[0],
                    relationships = results.relationships,
                    relationshipsTpl = [];

                relationships.forEach(function (relationship) {
                    var ontologyRelationship = ontologyRelationships.byTitle[relationship.title];
                    var displayName;
                    if (ontologyRelationship) {
                        displayName = ontologyRelationship.displayName;
                    } else {
                        displayName = relationship.title;
                    }

                    var data = {
                        title: relationship.title,
                        displayName: displayName
                    };

                    relationshipsTpl.push(data);
                });

                return relationshipsTpl;

            });
        };
    }
});
