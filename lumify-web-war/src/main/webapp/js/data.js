

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

    var WORKSPACE_SAVE_DELAY = 1500,
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
        this.workspaceId = null;

        this.defaultAttrs({
            droppableSelector: 'body'
        });


        this.after('initialize', function() {
            var self = this;

            this.setupAsyncQueue('workspace');
            this.setupAsyncQueue('relationships');
            this.setupAsyncQueue('socketSubscribe');
            this.setupDroppable();

            this.onSaveWorkspaceInternal = _.debounce(this.onSaveWorkspaceInternal.bind(this), WORKSPACE_SAVE_DELAY);
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

            this.on('copydocumenttext', this.onDocumentTextCopy);

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
                    //FIXME: need this?
                    //self.trigger('updateVertices', { vertices:[message.data.vertex]});
                    break;
            }
        };


        var copiedDocumentText,
            copiedDocumentTextStorageKey = 'SESSION_copiedDocumentText';
        this.onDocumentTextCopy = function(event, data) {
            copiedDocumentText = data;
            if (window.localStorage) {
                try {
                    localStorage.setItem(copiedDocumentTextStorageKey, JSON.stringify(data));
                } catch(e) {
                    console.warn('Unable to set localStorage item');
                }
            }
        };

        this.copiedDocumentText = function() {
            var text; 
            if (window.localStorage) {
                text = localStorage.getItem(copiedDocumentTextStorageKey);
                if (text) {
                    text = JSON.parse(text);
                }
            }

            if (text === null || _.isUndefined(text)) {
                return copiedDocumentText;
            }

            return text;
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

        this.throttledUpdatesByVertex = {};
        this.onSaveWorkspace = function(evt, data) {
            var self = this,
                updates = this.throttledUpdatesByVertex,
                vertexToEntityUpdate = function(vertex, updateType) {
                    if (~updateType.indexOf('Deletes')) {
                        return vertex.id;
                    }

                    return {
                        vertexId: vertex.id,
                        graphPosition: vertex.workspace.graphPosition
                    };
                },
                uniqueVertices = function(v) { return v.vertexId; };

            _.keys(data).forEach(function(key) {
                if (_.isArray(data[key])) {
                    data[key].forEach(function(vertex) {
                        updates[vertex.id] = {
                            updateType: key,
                            updateJson: vertexToEntityUpdate(vertex, key)
                        }
                    });
                }
            });

            this.onSaveWorkspaceInternal();
        };

        this.onSaveWorkspaceInternal = function() {
            var self = this;
            
            this.workspaceReady(function(ws) {
                this.trigger('workspaceSaving', ws);

                var updateJson = {}, updates = this.throttledUpdatesByVertex;
                _.keys(updates).forEach(function(vertexId) {
                    var update = updates[vertexId],
                        updateType = update.updateType;
                    (updateJson[updateType] || (updateJson[updateType] = [])).push(update.updateJson);
                });
                this.throttledUpdatesByVertex = {};

                this.workspaceService.save(this.workspaceId, updateJson).done(function(data) {
                   self.trigger('workspaceSaved', ws);
                   _.values(self.workspaceVertices).forEach(function(wv) {
                       delete wv.dropPosition;
                   });
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
                edge.properties.relationshipType,
                edge.id).done(function() {
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
                    if (!data.remoteEvent) self.trigger('saveWorkspace', { entityUpdates:added });
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
                    this.trigger('saveWorkspace', { entityUpdates:updated });
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
                selected = _.groupBy(vertices, function(v) { return v.concept ? 'vertices' : 'edges'; });

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
                    this.trigger('saveWorkspace', { entityDeletes:toDelete });
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
                        self.workspaceService.saveNew().done(function(workspace) {
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
            if (data.workspaceId != this.workspaceId) {
                this.loadWorkspace(data.workspaceId);
            }
        };

        this.onWorkspaceDeleted = function(evt, data) {
            if (this.workspaceId === data.workspaceId) {
                this.workspaceId = null;
                this.loadActiveWorkspace();
            }
        };

        this.onWorkspaceCopied = function (evt, data) {
            this.workspaceId = data.workspaceId;
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
            if (this.workspaceId == data.workspaceId) {
                // TODO: use activity to display message
            }
        };

        this.loadWorkspace = function(workspaceData) {
            var self = this,
                workspaceId = _.isString(workspaceData) ? workspaceData : workspaceData.workspaceId;

            self.workspaceId = workspaceId;

            // Queue up any requests to modify workspace
            self.workspaceUnload();
            self.relationshipsUnload();

            self.socketSubscribeReady(function() {
                self.getWorkspace(workspaceId).done(function(workspace) {
                    self.loadWorkspaceVertices(workspace).done(function(vertices) {
                        vertices.forEach(function(v) { delete v.dropPosition; });
                        workspace.data.vertices = vertices.sort(function(a,b) { 
                            if (a.workspace.graphPosition && b.workspace.graphPosition) return 0;
                            return a.workspace.graphPosition ? -1 : b.workspace.graphPosition ? 1 : 0;
                        });
                        workspace.data.verticesById = _.indexBy(vertices, 'id');

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
                        if (_.contains([403,404], xhr.status)) {
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
                    workspace.data.vertices = workspace.data.vertices || [];
                    workspace.data.verticesById = {};

                    return workspace;
                });
        };

        this.loadWorkspaceVertices = function(workspace) {
            var self = this,
                deferred = $.Deferred(),
                ids = _.keys(workspace.entities);

            _.each(_.values(self.cachedVertices), function(v) {
                v.workspace = {};
            });
            self.workspaceVertices = {};
            if (ids.length) {
                self.vertexService.getMultiple(ids).done(function(serverVertices) {

                    var vertices = serverVertices.map(function(vertex) {
                        var workspaceData = workspace.entities[vertex.id];
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

                            var properties = {};
                            _.keys(info).forEach(function(key) {
                                if ((/^(start|end|graphVertexId|type)$/).test(key)) return;
                                properties[key] = {
                                    value: info[key]
                                };
                            });
                            self.updateCacheWithVertex({
                                id: info.graphVertexId,
                                properties: properties
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
