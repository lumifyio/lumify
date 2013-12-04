define(['util/retina'], function(retina) {
    'with strict';

    return withControlDrag;

    function withControlDrag() {
        var controlKeyPressed = false,
            startControlDragTarget,
            tempTargetNode,
            currentEdgeId;

        this.defaultAttrs({
            createConnectionSelector: '.connect-dialog .create-connection'
        });

        this.after('initialize', function() {
            var self = this,
                lockedCyTarget;

            this.mouseDragHandler = self.onControlDragMouseMove.bind(this);

            this.on(document, 'controlKey', function() { controlKeyPressed = true; });
            this.on(document, 'controlKeyUp', function() { controlKeyPressed = false; });

            this.on('click', {
                createConnectionSelector: this.onCreateConnection
            });

            this.on('startVertexConnection', this.onStartVertexConnection);
            this.on('endVertexConnection', this.onEndVertexConnection);
            this.on('finishedVertexConnection', this.onFinishedVertexConnection);

            this.cytoscapeReady(function(cy) {
                cy.on({
                    tap: function(event) {
                        if (self.creatingStatement) {
                            this.trigger('finishedVertexConnection');
                        }
                    },
                    tapstart: function(event) {
                        if (self.creatingStatement) return;

                        if (controlKeyPressed && event.cyTarget !== cy) {
                            self.trigger('startVertexConnection', {
                                sourceId: event.cyTarget.id()
                            });
                        }
                    },
                    grab: function(event) {
                        if (controlKeyPressed) {
                            lockedCyTarget = event.cyTarget;
                            lockedCyTarget.lock();
                        }
                    },
                    free: function() {
                        if (self.creatingStatement) {
                            self.trigger('endVertexConnection', {
                                edgeId: currentEdgeId
                            });
                        }
                    }
                });
            });
        });

        this.onCreateConnection = function(e) {
            this.cytoscapeReady(function(cy) {
                var edge = cy.getElementById(currentEdgeId),
                    target = $(e.target),
                    dialog = target.closest('.connect-dialog');

                dialog.find('.form-button').hide();
                var form = dialog.find('.' + target.data('form')),
                    select = form.find('select');
                
                select.html('<option>Loading...</option>');
                form.find('.source').text(cy.getElementById(edge.data('source')).data('title'));
                form.find('button').attr('disabled', true);
                form.show();

                this.positionDialog(dialog);
                this.getRelationshipLabels(
                    cy.getElementById(edge.data('source')),
                    cy.getElementById(edge.data('target')),
                    select
                );
            });
        };

        this.onFinishedVertexConnection = function(event) {
            this.cytoscapeReady(function(cy) {
                cy.$('.temp').remove();
                cy.$('.controlDragSelection').removeClass('controlDragSelection');
                currentEdgeId = null;
                this.$node.find('.connect-dialog').hide();
                this.creatingStatement = false;
            });
        };

        this.showDialog = function(edge) {
            this.cytoscapeReady(function(cy) {
                var targetId = edge.data('target'),
                    target = cy.getElementById(targetId),
                    position = retina.pixelsToPoints(target.renderedPosition());

                position.y -= target.height() / cy.zoom() / 2;

                this.dialogPosition = {
                    x: position.x,
                    y: position.y
                };
                
                console.log(target.height(), cy.zoom());

                var dialog = this.$node.find('.connect-dialog'),
                    parent = dialog.parent('div');

                dialog.find('.form-button').show();
                dialog.find('.form').hide();

                parent.css({ position: 'absolute', left: position.x, top: position.y });
                this.positionDialog(dialog);
            });
        };

        this.positionDialog = function(dialog) {
            dialog.parent('div').css({
                left: this.dialogPosition.x - (dialog.outerWidth(true) / 2),
                top: this.dialogPosition.y - (dialog.outerHeight(true))
            });
            dialog.show();
        }

        this.onStartVertexConnection = function(event, data) {
            this.creatingStatement = true;
            this.cytoscapeReady(function(cy) {
                startControlDragTarget = cy.getElementById(data.sourceId);
                cy.nodes().lock();
                cy.on('mousemove', this.mouseDragHandler);
            });
        };

        this.onEndVertexConnection = function(event, data) {
            this.cytoscapeReady(function(cy) {
                cy.off('mousemove', this.mouseDragHandler);
                cy.nodes().unlock();
                startControlDragTarget = null;

                var edge = currentEdgeId && cy.getElementById(currentEdgeId);
                if (edge && !cy.getElementById(edge.data('target')).hasClass('temp')) {
                    return this.showDialog(edge);
                }

                this.trigger('finishedVertexConnection');
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
                if (!edge.length) {
                    console.log(sourceId, targetId);
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
        
        this.getRelationshipLabels = function (source, dest, select) {
            var self = this;
            var sourceConceptTypeId = source.data('_subType');
            var destConceptTypeId = dest.data('_subType');
            self.ontologyService.conceptToConceptRelationships(sourceConceptTypeId, destConceptTypeId)
                .done(function(results) {
                    self.displayRelationships(results.relationships, select);
                });
        };

        this.displayRelationships = function (relationships, select) {
            var self = this;
            self.ontologyService.relationships()
                .done(function(ontologyRelationships) {

                    var relationshipsTpl = [];

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

                    if (relationshipsTpl.length) {
                        select.html(
                            relationshipsTpl.map(function(d){
                                return '<option value="' + d.title + '">' + d.displayName + '</option>';
                            }).join()
                        )
                    } else {
                        select.html('<option>No valid relationships</option>');
                    }

                    select.siblings('button').removeAttr('disabled');
                    _.delay(function() {
                    self.positionDialog(select.closest('.connect-dialog'));
                    }, 2000)
                });
        };
    }
});
