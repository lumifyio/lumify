
define([
    'require',
    'jscache'
], function(require, Cache) {
    'use strict';

        // Object cache per workspace
    var KIND_TO_CACHE = {
            vertex: 'vertices',
            edge: 'edges',
            workspace: 'workspace'
        },
        workspaceCaches = {},

        api = {

            getObject: function(workspaceId, kind, objectId) {
                var result = api.getObjects(workspaceId, kind, objectId ? [objectId] : null);
                if (objectId) {
                    return result.length ? result[0] : null;
                }
                return result;
            },

            getObjects: function(workspaceId, kind, objectIds) {
                if (!(kind in KIND_TO_CACHE)) {
                    throw new Error('kind parameter not valid', kind);
                }

                var workspaceCache = cacheForWorkspace(workspaceId, { create: false }),
                    cache = workspaceCache && workspaceCache[KIND_TO_CACHE[kind]];

                if (objectIds) {
                    return objectIds.map(function(oId) {
                        return cache && cache.getItem(oId);
                    });
                }

                return cache;
            },

            removeObject: function(workspaceId, kind, objectId) {
                if (!(kind in KIND_TO_CACHE)) {
                    throw new Error('kind parameter not valid', kind);
                }

                var workspaceCache = cacheForWorkspace(workspaceId, { create: false }),
                    cache = workspaceCache && workspaceCache[KIND_TO_CACHE[kind]];

                if (cache && objectId) {
                    cache.removeItem(objectId);
                }
            },

            setWorkspace: function(workspace) {
                workspace.vertices = _.indexBy(workspace.vertices, 'vertexId');
                var workspaceCache = cacheForWorkspace(workspace.workspaceId);
                workspaceCache.workspace = workspace;
                return workspace;
            },

            updateWorkspace: function(workspaceId, changes) {
                var workspace = JSON.parse(JSON.stringify(api.getObject(workspaceId, 'workspace')));
                changes.entityUpdates.forEach(function(entityUpdate) {
                    var workspaceVertex = _.findWhere(workspace.vertices, { vertexId: entityUpdate.vertexId });
                    if (workspaceVertex) {
                        workspaceVertex.graphPosition = entityUpdate.graphPosition;
                    } else {
                        workspace.vertices[entityUpdate.vertexId] = {
                            vertexId: entityUpdate.vertexId,
                            graphPosition: entityUpdate.graphPosition
                        };
                    }
                });
                workspace.vertices = _.omit(workspace.vertices, changes.entityDeletes);
                if (changes.title) {
                    workspace.title = changes.title
                }

                var updatedIds = _.pluck(changes.userUpdates, 'userId');
                updatedIds.concat(changes.userDeletes).forEach(function(userId) {
                    for (var i = 0; i < workspace.users.length; i++) {
                        if (workspace.users[i].userId === userId) {
                            workspace.users.splice(i, 1);
                            return;
                        }
                    }
                });
                workspace.users = workspace.users.concat(changes.userUpdates);

                api.workspaceWasChangedRemotely(workspace);
                return workspace;
            },

            removeWorkspace: function(workspaceId) {
                if (workspaceId in workspaceCaches) {
                    delete workspaceCaches[workspaceId];
                }
            },

            workspaceWasChangedRemotely: function(remoteWorkspace) {
                var user = _.findWhere(remoteWorkspace.users, { userId: publicData.currentUser.id });
                remoteWorkspace.editable = /WRITE/i.test(user && user.access);
                remoteWorkspace.isSharedToUser = remoteWorkspace.createdBy !== publicData.currentUser.id;
                if (('vertices' in remoteWorkspace)) {
                    remoteWorkspace.vertices = _.indexBy(remoteWorkspace.vertices, 'vertexId');
                }
                var workspace = api.getObject(remoteWorkspace.workspaceId, 'workspace');
                if (!workspace || !_.isEqual(remoteWorkspace, workspace)) {
                    console.debug('WORKSPACE UPDATED', remoteWorkspace, workspace)

                    var vertexIds = _.keys(remoteWorkspace.vertices),
                        vertexIdsPrevious = workspace ? _.keys(workspace.vertices) : [],
                        addedIds = _.difference(vertexIds, vertexIdsPrevious),
                        removedIds = _.difference(vertexIdsPrevious, vertexIds),
                        updatedIds = _.without.apply(_, [vertexIds].concat(addedIds)),
                        added = _.values(_.pick(remoteWorkspace.vertices, addedIds)),
                        updated = _.compact(_.map(updatedIds, function(vId) {
                            return (workspace && _.isEqual(
                                remoteWorkspace.vertices[vId].graphPosition,
                                workspace.vertices[vId].graphPosition
                            )) ? null : remoteWorkspace.vertices[vId];
                        }));

                    require(['../services/vertex'], function(vertex) {
                        vertex.store({ vertexIds: addedIds })
                            .done(function(newVertices) {
                                api.setWorkspace(remoteWorkspace);
                                dispatchMain('workspaceUpdated', {
                                    workspace: remoteWorkspace,
                                    newVertices: newVertices,
                                    entityUpdates: updated.concat(added),
                                    entityDeletes: removedIds,
                                    userUpdates: [],
                                    userDeletes: []
                                });
                            });
                    });
                }
            },

            workspaceShouldSave: function(workspace, changes) {
                var willChange = false;

                willChange = willChange || (changes.title && changes.title !== workspace.title);

                willChange = willChange || changes.userDeletes.length;
                willChange = willChange || changes.userUpdates.length;

                willChange = willChange || changes.entityDeletes.length;
                willChange = willChange || _.any(changes.entityUpdates, function(entityUpdate) {
                    var workspaceVertex = _.findWhere(workspace.vertices, { vertexId: entityUpdate.vertexId });
                    if (workspaceVertex) {
                        return !_.isEqual(workspaceVertex.graphPosition, entityUpdate.graphPosition);
                    } else {
                        return true;
                    }
                });

                return willChange;
            },

            updateObject: function(data, options) {
                var onlyIfExists = options && options.onlyIfExists === true,
                    cached;

                if (!data.workspaceId) {
                    data.workspaceId = publicData.currentWorkspaceId;
                }

                if (data.vertex && resemblesVertex(data.vertex)) {
                    cached = api.getObject(data.workspaceId, 'vertex', data.vertex.id)
                    if (cached || !onlyIfExists) {
                        cacheVertices(data.workspaceId, [data.vertex]);
                    }
                }

                if (data.edge) {
                    var toCache;

                    if (resemblesEdge(data.edge)) {
                        toCache = data.edge;
                    } else {
                        if (data.edge.sourceVertexId && data.edge.destVertexId) {
                            // Load vertices from cache
                            cached = _.compact(api.getObjects(data.workspaceId, 'vertex', [
                                data.edge.sourceVertexId,
                                data.edge.destVertexId
                            ]));
                            if (cached && cached.length === 2) {
                                toCache = _.extend({}, _.omit(data.edge, 'sourceVertexId', 'destVertexId'), {
                                    source: cached[0],
                                    target: cached[1]
                                });
                            }
                        }
                    }

                    if (toCache) {
                        cacheEdges(data.workspaceId, [toCache]);
                    }
                }
            },

            checkAjaxForPossibleCaching: function(xhr, json, workspaceId, request) {
                if (resemblesVertex(json)) {
                    cacheVertices(workspaceId, [json]);
                }
                if (resemblesVertices(json.vertices)) {
                    cacheVertices(workspaceId, json.vertices);
                }
                if (resemblesEdge(json)) {
                    cacheEdges(workspaceId, [json]);
                }
                if (resemblesEdges(json.edges)) {
                    cacheEdges(workspaceId, json.edges);
                }
            }
        };

    return api;

    function cacheForWorkspace(workspaceId, options) {
        if (!workspaceId) {
            workspaceId = publicData.currentWorkspaceId;
        }

        if (workspaceId in workspaceCaches) {
            return workspaceCaches[workspaceId];
        }

        if (options && options.create === false) {
            return null;
        }

        workspaceCaches[workspaceId] = {
            vertices: new Cache(),
            edges: new Cache()
        };

        return workspaceCaches[workspaceId];
    }

    function cacheVertices(workspaceId, vertices) {
        var vertexCache = cacheForWorkspace(workspaceId).vertices,
            updated = _.compact(vertices.map(function(v) {

                // Search puts a score, but we don't use it and it breaks
                // our cache update check
                if ('score' in v) {
                    delete v.score;
                }

                var previous = vertexCache.getItem(v.id);
                if (previous && _.isEqual(v, previous)) {
                    return;
                }

                console.debug('Cache ', v.id, workspaceId)
                vertexCache.setItem(v.id, v, {
                    expirationAbsolute: null,
                    expirationSliding: null,
                    priority: Cache.Priority.HIGH,
                    callback: function(k, v) {
                        console.debug('removed ' + k);
                    }
                });

                if (previous) {
                    console.debug('Vertex updated previous:', previous, 'new:', v)
                    return v;
                }
            }));

        if (updated.length && workspaceId === publicData.currentWorkspaceId) {
            // TODO: somehow also update vertices from other workspaces?
            console.log('updated', updated)
            dispatchMain('storeObjectsUpdated', { vertices: updated });
        }
    }

    function cacheEdges(workspaceId, edges) {
        var edgeCache = cacheForWorkspace(workspaceId).edges,
            workspace = api.getObject(workspaceId, 'workspace'),
            updated = _.compact(edges.map(function(e) {
                var previous = edgeCache.getItem(e.id),
                    bothVerticesInWorkspace = workspace &&
                        e.source.id in workspace.vertices &&
                        e.target.id in workspace.vertices;

                if ((previous && _.isEqual(e, previous)) || !bothVerticesInWorkspace) {
                    return;
                }

                console.debug('Cache ', e.id, workspaceId)
                edgeCache.setItem(e.id, e, {
                    expirationAbsolute: null,
                    expirationSliding: null,
                    priority: Cache.Priority.HIGH,
                    callback: function(k, v) {
                        console.debug('removed ' + k);
                    }
                });

                console.debug('Edge updated previous:', previous, 'new:', e)
                return e;
            }));

        if (updated.length && workspaceId === publicData.currentWorkspaceId) {
            console.log('updated', updated)
            dispatchMain('storeObjectsUpdated', { edges: updated });
        }
    }

    function resemblesEdge(val) {
        return (
            _.isObject(val) &&
            val.type === 'edge' &&
            _.has(val, 'id') &&
            _.has(val, 'label') &&
            !_.isEmpty(val.source) &&
            !_.isEmpty(val.target)
        );
    }

    function resemblesEdges(val) {
        return _.isArray(val) && val.length && resemblesEdge(val[0]);
    }

    function resemblesVertex(val) {
        return (
            _.isObject(val) &&
            _.has(val, 'id') &&
            _.has(val, 'sandboxStatus') &&
            _.has(val, 'properties') &&
            _.isArray(val.properties) &&
            !_.has(val, 'sourceVertexId') &&
            !_.has(val, 'destVertexId')
        );
    }

    function resemblesVertices(val) {
        return _.isArray(val) && val.length && resemblesVertex(val[0]);
    }
});
