

define([
    'flight/lib/component',
    'flight/lib/registry',
    'data/withVertexCache',
    'data/withAjaxFilters',
    'util/withAsyncQueue',
    'util/keyboard',
    'service/workspace',
    'service/vertex',
    'service/ontology',
    'util/undoManager',
    'util/clipboardManager'
], function(
    // Flight
    defineComponent, registry,
    // Mixins
    withVertexCache, withAjaxFilters, withAsyncQueue, 
    // Service
    Keyboard, WorkspaceService, VertexService, OntologyService, undoManager, ClipboardManager) {
    'use strict';

    var WORKSPACE_SAVE_DELAY = 1000,
        RELOAD_RELATIONSHIPS_DELAY = 250,
        ADD_VERTICES_DELAY = 100;

    return initializeData();

    function initializeData() {
        var DataComponent = defineComponent(Data, withAsyncQueue, withVertexCache, withAjaxFilters);
        DataComponent.attachTo(document);

        var instanceInfo = _.find(registry.findInstanceInfoByNode(document), function(info) {
            return info.instance.constructor === DataComponent;
        });

        if (instanceInfo) {
            return instanceInfo.instance;
        } else {
            throw "Unable to find data instance";
        }
    }

    function freeze(obj) {
        if (_.isArray(obj)) {
            return _.map(obj, function(v) {
                return Object.freeze(Object.create(v));
            });
        } else if (obj.vertices) {
            obj.vertices = Object.freeze(obj.vertices);
        }

        return Object.freeze(obj);
    }

    function Data() {

        this.workspaceService = new WorkspaceService();
        this.vertexService = new VertexService();
        this.ontologyService = new OntologyService();
        this.selectedVertices = [];
        this.selectedVertexIds = [];
        this.id = null;

        this.defaultAttrs({
            droppableSelector: 'body'
        });


        this.after('initialize', function() {
            var self = this;

            this.setupAsyncQueue('workspace');
            this.setupAsyncQueue('relationships');
            this.setupAsyncQueue('socketSubscribe');
            this.setupDroppable();

            this.onSaveWorkspace = _.debounce(this.onSaveWorkspace.bind(this), WORKSPACE_SAVE_DELAY);
            this.refreshRelationships = _.debounce(this.refreshRelationships.bind(this), RELOAD_RELATIONSHIPS_DELAY);

            this.cachedConceptsDeferred = $.Deferred();
            this.ontologyService.concepts().done(function(concepts) {
                self.cachedConcepts = concepts;
                self.cachedConceptsDeferred.resolve(concepts);
            })

            ClipboardManager.attachTo(this.node);
            Keyboard.attachTo(this.node);

            // Vertices
            this.on('addVertices', this.onAddVertices);
            this.on('updateVertices', this.onUpdateVertices);
            this.on('deleteVertices', this.onDeleteVertices);
            this.on('refreshRelationships', this.refreshRelationships);
            this.on('selectObjects', this.onSelectObjects);
            this.on('clipboardPaste', this.onClipboardPaste);
            this.on('clipboardCut', this.onClipboardCut);
            this.on('deleteEdges', this.onDeleteEdges);

            // Workspaces
            this.on('saveWorkspace', this.onSaveWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
            this.on('workspaceDeleted', this.onWorkspaceDeleted);
            this.on('workspaceDeleting', this.onWorkspaceDeleting);
            this.on('workspaceCopied', this.onWorkspaceCopied);
            this.on('reloadWorkspace', this.onReloadWorkspace);

            this.on('socketMessage', this.onSocketMessage);

            this.on('selectAll', this.onSelectAll);
            this.on('deleteSelected', this.onDelete);

            this.on('applicationReady', function() {
                self.cachedConceptsDeferred.done(function() {
                    self.onApplicationReady();
                });
            });
        });

        this.onApplicationReady = function() {
            var self = this;

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['Graph', 'Map'],
                shortcuts: {
                    'meta-a': { fire:'selectAll', desc:'Select all vertices' },
                    'delete': { fire:'deleteSelected', desc:'Removes selected vertices from workspace, deletes selected relationships'}
                }
            });

            this.workspaceService.subscribe({
                onMessage: function (err, message) {
                    if (err) {
                        console.error('Error', err);
                        return self.trigger(document, 'error', { message: err.toString() });
                    }
                    if (message) {
                        self.trigger(document, 'socketMessage', message);
                    }
                },
                onOpen: function(response) {
                    self.trigger(document, 'subscribeSocketOpened');
                    self.socketSubscribeMarkReady(response);
                }
            });
        };

        this.onSocketMessage = function (evt, message) {
            var self = this;
            switch (message.type) {
                case 'propertiesChange':
                    self.trigger('updateVertices', { vertices:[message.data.vertex]});
                    break;
            }
        };

        this.onSelectAll = function() {
            this.trigger('selectObjects', { vertices:this.verticesInWorkspace() });
        };

        this.onDelete = function() {
            if (this.selectedVertices.length) {
                this.trigger('deleteVertices', { vertices: this.vertices(this.selectedVertices)})
            } else if (this.selectedEdges && this.selectedEdges.length) {
                this.trigger('deleteEdges', { edges: this.selectedEdges});
            }
        };

        this.onSaveWorkspace = function(evt, workspace) {
            var self = this,
                saveFn;

            if (self.id) {
                saveFn = self.workspaceService.save.bind(self.workspaceService, self.id);
            } else {
                saveFn = self.workspaceService.saveNew.bind(self.workspaceService);
            }

            self.workspaceReady(function(ws) {
                _.values(self.workspaceVertices).forEach(function(wv) {
                    delete wv.dropPosition;
                });

                self.trigger('workspaceSaving', ws);

                saveFn({ data:{ workspaceVertices:self.workspaceVertices }}).done(function(data) {
                    self.id = data._rowKey;
                    self.trigger('workspaceSaved', data);
                });
            });
        };


        this.refreshRelationships = function() {
            var self = this,
                ids = this.getIds();

            this.relationshipsUnload();

            this.vertexService.getRelationships(ids)
                .done(function(relationships) {
                    self.relationshipsMarkReady(relationships);
                    self.trigger('relationshipsLoaded', { relationships: relationships });
                });
        };

        this.onDeleteEdges = function(evt, data) {
            if (!data.edges || !data.edges.length) {
                return console.error('Invalid event data to delete edge', data);
            }

            var self = this,
                edge = data.edges[0];

            this.vertexService.deleteEdge(
                edge.properties.source,
                edge.properties.target,
                edge.properties.relationshipType).done(function() {
                    if (_.findWhere(self.selectedEdges, { id:edge.id })) {
                        self.trigger('selectObjects');
                    }
                    self.trigger('edgesDeleted', { edgeId:edge.id });
                });
        };


        this.onAddVertices = function(evt, data) {
            this.workspaceReady(function(ws) {
                if (!ws.isEditable && !data.remoteEvent) return;

                var self = this,
                    added = [],
                    existing = [];


                // Check if vertices are missing properties (from search results)
                var needsRefreshing = data.vertices.filter(function(v) { 
                        var cached = self.vertex(v.id);
                        if (!cached) {
                            return !v.properties || !v.properties._refreshedFromServer;
                        }
                        return !cached.properties._refreshedFromServer;
                    }),
                    passedWorkspace = {};

                data.vertices.forEach(function(v) {
                    v.workspace = v.workspace || {
                        selected: true
                    };
                    passedWorkspace[v.id] = self.copy(v.workspace);
                });

                var deferred = $.Deferred();
                if (needsRefreshing.length) {
                    this.vertexService.getMultiple(_.pluck(needsRefreshing, 'id')).done(function(vertices) {
                        deferred.resolve(data.vertices);
                    });
                } else deferred.resolve(data.vertices);

                deferred.done(function(vertices) {
                    vertices = self.vertices(vertices);

                    vertices.forEach(function(vertex) {
                        vertex.properties._refreshedFromServer = true;
                        if (passedWorkspace[vertex.id]) {
                            vertex.workspace = $.extend(vertex.workspace, passedWorkspace[vertex.id]);
                        }

                        var inWorkspace = self.workspaceVertices[vertex.id];
                        var cache = self.updateCacheWithVertex(vertex);

                        self.workspaceVertices[vertex.id] = cache.workspace;

                        if (inWorkspace) {
                            existing.push(cache);
                        } else {
                            added.push(cache);
                        }
                    });

                    if (existing.length) self.trigger('existingVerticesAdded', { vertices:freeze(existing) });

                    if (added.length === 0) {
                        var message = "No New Vertices Added";
                        self.trigger('displayInformation', { message:message });
                        return;
                    }

                    if(!data.noUndo) {
                        var dataClone = JSON.parse(JSON.stringify(data));
                        dataClone.noUndo = true;
                        undoManager.performedAction( 'Add ' + dataClone.vertices.length + ' vertices', {
                            undo: function() {
                                self.trigger('deleteVertices', dataClone);
                            },
                            redo: function() {
                                self.trigger('addVertices', dataClone);
                            }
                        });
                    }

                    self.trigger('refreshRelationships');
                    if (!data.remoteEvent) self.trigger('saveWorkspace');
                    if (added.length) {
                        ws.data.vertices = ws.data.vertices.concat(added);
                        self.trigger('verticesAdded', { 
                            vertices:freeze(added),
                            remoteEvent: data.remoteEvent,
                            options: data.options || {}
                        });
                    }
                });
            });
        };


        this.onUpdateVertices = function(evt, data) {
            var self = this;

            this.workspaceReady(function(ws) {
                var undoData = { noUndo: true, vertices: [] };
                var redoData = { noUndo: true, vertices: [] };

                var shouldSave = false,
                    updated = data.vertices.map(function(vertex) {
                        if (!vertex.id && vertex.graphVertexId) {
                            vertex = {
                                id: vertex.graphVertexId,
                                properties: vertex
                            };
                        }

                        // Only save if workspace updated
                        if (self.workspaceVertices[vertex.id] && vertex.workspace) {
                            shouldSave = true;
                        }


                        if (shouldSave) undoData.vertices.push(self.workspaceOnlyVertexCopy({id:vertex.id}));
                        var cache = self.updateCacheWithVertex(vertex);
                        if (shouldSave) redoData.vertices.push(self.workspaceOnlyVertexCopy(cache));
                        return cache;
                    });

                if(!data.noUndo && undoData.vertices.length) {
                    undoManager.performedAction( 'Update ' + undoData.vertices.length + ' vertices', {
                        undo: function() { self.trigger('updateVertices', undoData); },
                        redo: function() { self.trigger('updateVertices', redoData); }
                    });
                }

                if (shouldSave && !data.remoteEvent) {
                    this.trigger('saveWorkspace');
                }
                if (updated.length) {
                    this.trigger('verticesUpdated', { 
                        vertices:freeze(updated),
                        remoteEvent: data.remoteEvent
                    });
                }
            });
        };

        this.getVerticesFromClipboardData = function(data) {
            if (data) {
                var vertexUrlMatch = data.match(/#v=([0-9,]+)$/);
                if (vertexUrlMatch) {
                    return vertexUrlMatch[1].split(',');
                }
            }

            return [];
        };

        this.formatVertexAction = function(action, vertices) {
            var len = vertices.length,
                plural = len === 1 ? 'vertex' : 'vertices';
            return (action + ' ' + len + ' ' + plural);
        };

        this.onClipboardCut = function(evt, data) {
            var self = this,
                vertexIds = this.getVerticesFromClipboardData(data.data).filter(function(vId) {
                    // Only cut from those in workspace
                    return self.workspaceVertices[vId];
                }),
                len = vertexIds.length;

            if (len) {
                this.trigger('deleteVertices', { vertices: this.vertices(vertexIds) });
                this.trigger('displayInformation', { message:this.formatVertexAction('Cut', vertexIds)});
            }
        };

        this.onClipboardPaste = function(evt, data) {
            var self = this,
                vertexIds = this.getVerticesFromClipboardData(data.data).filter(function(vId) {
                    // Only allow paste from vertices not in workspace
                    return !self.workspaceVertices[vId];
                }),
                len = vertexIds.length,
                plural = len === 1 ? 'vertex' : 'vertices';

            if (len) {
                this.trigger('displayInformation', { message:this.formatVertexAction('Paste', vertexIds)});
                this.vertexService.getMultiple(vertexIds).done(function(serverVertices) {
                    self.trigger('addVertices', { vertices:serverVertices });
                });
            }
        };

        this.onSelectObjects = function(evt, data) {
            if (data && data.remoteEvent) return;

            var self = this,
                vertices = data && data.vertices || [],
                selectedIds = _.pluck(vertices, 'id'),
                selected = _.groupBy(vertices, function(v) { return v.properties._type === 'relationship' ? 'edges' : 'vertices'; });

            selected.vertices = selected.vertices || [];
            selected.edges = selected.edges || [];

            if (selected.vertices.length) {
                this.trigger('clipboardSet', {
                    text: window.location.href.replace(/#.*$/,'') + '#v=' + _.pluck(selected.vertices, 'id').join(',')
                });
            } else {
                this.trigger('clipboardClear');
            }

            this.selectedVertices = selected.vertices;
            this.selectedVertexIds = _.pluck(selected.vertices, 'id');
            this.selectedEdges = selected.edges;

            _.keys(this.workspaceVertices).forEach(function(id) {
                var info = self.workspaceVertices[id];
                info.selected = selectedIds.indexOf(id) >= 0;
            });

            this.trigger('objectsSelected', selected);
        };

        this.onDeleteVertices = function(evt, data) {
            var self = this;
            this.workspaceReady(function(ws) {
                if (!ws.isEditable && !data.remoteEvent) return;

                var toDelete = [],
                    undoDelete = [],
                    redoDelete = [];
                data.vertices.forEach(function(deletedVertex) {
                    var workspaceInfo = self.workspaceVertices[deletedVertex.id];
                    if (workspaceInfo) {
                        redoDelete.push(self.workspaceOnlyVertexCopy(deletedVertex.id));
                        undoDelete.push(self.copy(self.vertex(deletedVertex.id)));
                        toDelete.push(self.vertex(deletedVertex.id));

                        delete self.workspaceVertices[deletedVertex.id];
                    }
                    var cache = self.vertex(deletedVertex.id);
                    if (cache) {
                        cache.workspace = {};
                    }
                });

                if(!data.noUndo && undoDelete.length) {
                    undoManager.performedAction( 'Delete ' + toDelete.length + ' vertices', {
                        undo: function() { self.trigger(document, 'addVertices', { noUndo:true, vertices:undoDelete }); },
                        redo: function() { self.trigger(document, 'deleteVertices', { noUndo:true, vertices:redoDelete }); }
                    });
                }

                if (!data.remoteEvent) {
                    this.trigger('saveWorkspace');
                }
                if (toDelete.length) {
                    var ids = _.pluck(toDelete, 'id');
                    ws.data.vertices = _.filter(ws.data.vertices, function(v) {
                        return ids.indexOf(v.id) === -1;
                    });
                    this.trigger('verticesDeleted', { 
                        vertices:freeze(toDelete),
                        remoteEvent: data.remoteEvent
                    });
                }
            });
        };

        this.loadActiveWorkspace = function() {
            var self = this;
            return self.workspaceService.list()
                .done(function(data) {
                    var workspaces = data.workspaces || [],
                        myWorkspaces = _.filter(workspaces, function(w) { 
                            return !w.isSharedToUser;
                        });

                    if (myWorkspaces.length === 0) {
                        self.workspaceService.saveNew({data:{}}).done(function(workspace) {
                            self.loadWorkspace(workspace);
                        });
                        return;
                    }

                    for (var i = 0; i < workspaces.length; i++) {
                        if (workspaces[i].active) {
                            return self.loadWorkspace(workspaces[i]);
                        }
                    }

                    self.loadWorkspace(myWorkspaces[0]);
                });
        };

        this.onSwitchWorkspace = function(evt, data) {
            if (data._rowKey != this.id) {
                this.loadWorkspace(data._rowKey);
            }
        };

        this.onWorkspaceDeleted = function(evt, data) {
            if (this.id === data._rowKey) {
                this.id = null;
                this.loadActiveWorkspace();
            }
        };

        this.onWorkspaceCopied = function (evt, data) {
            this.id = data._rowKey;
            this.loadActiveWorkspace();
        }

        this.onReloadWorkspace = function(evt, data) {
            this.workspaceReady(function(workspace) {
                this.relationshipsReady(function(relationships) {
                    this.trigger('workspaceLoaded', freeze(workspace));
                    this.trigger('relationshipsLoaded', { relationships:relationships });
                });
            });
        };

        this.onWorkspaceDeleting = function (evt, data) {
            if (this.id == data._rowKey) {
                // TODO: use activity to display message
            }
        };

        this.loadWorkspace = function(workspaceData) {
            var self = this,
                workspaceRowKey = _.isString(workspaceData) ? workspaceData : workspaceData._rowKey;

            self.id = workspaceRowKey;

            // Queue up any requests to modify workspace
            self.workspaceUnload();
            self.relationshipsUnload();

            self.socketSubscribeReady(function() {
                self.getWorkspace(workspaceRowKey).done(function(workspace) {
                    self.loadWorkspaceVertices(workspace).done(function(vertices) {
                        if (workspaceData && workspaceData.title) {
                            workspace.title = workspaceData.title;
                        }
                        vertices.forEach(function(v) { delete v.dropPosition; });
                        workspace.data.vertices = vertices.sort(function(a,b) { 
                            if (a.workspace.graphPosition && b.workspace.graphPosition) return 0;
                            return a.workspace.graphPosition ? -1 : b.workspace.graphPosition ? 1 : 0;
                        });
                        workspace.data.verticesById = _.groupBy(vertices, 'id');

                        self.workspaceMarkReady(workspace);                        
                        self.trigger('workspaceLoaded', freeze(workspace));
                    });
                });
            });
        };

        this.getWorkspace = function(id) {
            var self = this,
                deferred = $.Deferred();

            if (id) {
                self.workspaceService.getByRowKey(id)
                    .fail(function(xhr) {
                        if (xhr.status === 404) {
                            self.trigger('workspaceNotAvailable');
                            self.loadActiveWorkspace();
                        }
                        deferred.reject();
                    })
                    .done(function(workspace) { 
                        deferred.resolve(workspace); 
                    });
            } else {
                deferred.resolve();
            }
            return deferred.then(function(workspace) {
                    workspace = workspace || {};
                    workspace.data = workspace.data || {};
                    workspace.data.workspaceVertices = workspace.data.workspaceVertices || {};
                    workspace.data.vertices = workspace.data.vertices || [];
                    workspace.data.verticesById = {};

                    return workspace;
                });
        };

        this.loadWorkspaceVertices = function(workspace) {
            var self = this,
                deferred = $.Deferred(),
                ids = _.keys(workspace.data.workspaceVertices);

            _.each(_.values(self.cachedVertices), function(v) {
                v.workspace = {};
            });
            self.workspaceVertices = {};
            if (ids.length) {
                self.vertexService.getMultiple(ids).done(function(serverVertices) {

                    var vertices = serverVertices.map(function(vertex) {
                        var workspaceData = workspace.data.workspaceVertices[vertex.id];
                        delete workspaceData.dropPosition;

                        var cache = self.updateCacheWithVertex(vertex);
                        cache.properties._refreshedFromServer = true;
                        cache.workspace = workspaceData || {};
                        cache.workspace.selected = false;
                        self.workspaceVertices[vertex.id] = cache.workspace;

                        workspace.data.verticesById[vertex.id] = cache;
                        return cache;
                    });
                    workspace.data.vertices = vertices;

                    undoManager.reset();

                    self.refreshRelationships();
                    deferred.resolve(vertices);
                });
            } else {
                deferred.resolve([]);
            }

            return deferred;
        };


        this.getIds = function () {
            return Object.keys(this.workspaceVertices);
        };

        this.setupDroppable = function() {
            var self = this;

            var enabled = false,
                droppable = this.select('droppableSelector');

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
                over: function( event, ui ) {
                    var draggable = ui.draggable,
                        start = true,
                        graphVisible = $('.graph-pane-2d').is('.visible'),
                        dashboardVisible = $('.dashboard-pane').is('.visible'),
                        vertices,
                        wrapper = $('.draggable-wrapper');

                    // Prevent map from swallowing mousemove events by adding
                    // this transparent full screen div
                    if (wrapper.length === 0) {
                        wrapper = $('<div class="draggable-wrapper"/>').appendTo(document.body);
                    }

                    draggable.off('drag.droppable-tracking');
                    draggable.on('drag.droppable-tracking', function(event, draggableUI) {
                        if (!vertices) {
                            vertices = verticesFromDraggable(draggable);
                        }
                        
                        if (graphVisible) {
                            ui.helper.toggleClass('draggable-invisible', enabled);
                        } else if (dashboardVisible) {
                            self.trigger('menubarToggleDisplay', { name:'graph' });
                            dashboardVisible = false;
                            graphVisible = true;
                        }

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
                drop: function( event, ui ) {
                    $('.draggable-wrapper').remove();

                    // Early exit if should leave to a different droppable
                    if (!enabled) return;

                    var vertices = verticesFromDraggable(ui.draggable),
                        graphVisible = $('.graph-pane-2d').is('.visible');

                    if (graphVisible && vertices.length) {
                        vertices[0].workspace.dropPosition = { x: event.clientX, y: event.clientY };
                    }

                    self.workspaceReady(function(ws) {
                        if (ws.isEditable) {
                            self.trigger('verticesDropped', { vertices:vertices });
                        }
                    });
                }.bind(this)
            });

            function verticesFromDraggable(draggable) {
                var alsoDragging = draggable.data('ui-draggable').alsoDragging,
                    anchors = draggable;

                if (alsoDragging && alsoDragging.length) {
                    anchors = draggable.add(alsoDragging.map(function(i, a) {
                        return a.data('original');
                    }));
                }

                return anchors.map(function(i, a) {
                    a = $(a);
                    var id = a.data('vertexId') || a.closest('li').data('vertexId');
                    if (!id) {

                        // Highlighted entities (legacy info)
                        var info = a.data('info') || a.closest('li').data('info');
                        if (info && info.graphVertexId) {

                            self.updateCacheWithVertex({
                                id: info.graphVertexId,
                                properties: _.omit(info, 'start', 'end', 'graphVertexId')
                            });
                            id = info.graphVertexId;
                        } 
                        
                        if (!id) return console.error('No data-vertex-id attribute for draggable element found', a[0]);
                    }

                    return self.vertex(id);
                }).toArray();
            }
        };
    }
});
