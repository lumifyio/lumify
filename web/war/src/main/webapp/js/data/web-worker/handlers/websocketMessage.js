define(['require'], function(require) {
    'use strict';

    var NOOP = function() {},
        storeTypeForData = function(data) {
            return ('graphVertexId' in data) ? 'vertex' : ('graphEdgeId' in data) ? 'edge' : null;
        },
        socketHandlers = {
            workspaceChange: function(data, json) {
                require(['../util/store'], function(store) {
                    store.workspaceWasChangedRemotely(data);
                })
            },
            workspaceDelete: function(data) {
                require([
                    '../util/store',
                    './workspaceSwitch'
                ], function(store, workspaceSwitch) {
                    store.removeWorkspace(data.workspaceId);
                    workspaceSwitch(data);
                    dispatchMain('rebroadcastEvent', {
                        eventName: 'workspaceDeleted',
                        data: {
                            workspaceId: data.workspaceId
                        }
                    })
                });
            },
            sessionExpiration: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'sessionExpiration'
                });
            },
            userStatusChange: (function() {
                // TODO: put into store
                var previousById = {};
                return function(data) {
                    var previous = data && previousById[data.id];
                    if (!previous || !_.isEqual(data, previous)) {
                        previousById[data.id] = data;
                        dispatchMain('rebroadcastEvent', {
                            eventName: 'userStatusChange',
                            data: data
                        });
                    }
                }
            })(),
            userWorkspaceChange: NOOP,
            publish: function(data) {
                // Property undo already publishes propertyChange
                if (data.objectType !== 'property' || data.publishType !== 'undo') {
                    socketHandlers.propertyChange(data);
                }
            },
            propertyChange: function(data) {
                var type = storeTypeForData(data),
                    objectId = type && (data.graphVertexId || data.graphEdgeId);

                if (!type) {
                    throw new Error('Property change sent unknown type', data);
                }

                require(['../util/store'], function(store) {
                    var storeObject = store.getObject(publicData.currentWorkspaceId, type, objectId),
                        edgeCreation = type === 'edge' && !('propertyName' in data);
                    if (storeObject || edgeCreation) {
                        require(['../services/' + type], function(service) {
                            service.properties(objectId)
                                .catch(function(error) {
                                    // Ignore 404's since we need to check if
                                    // we have access to changed object
                                    if (!error || error.status !== 404) {
                                        throw error;
                                    }
                                }).done();
                        });
                    }
                });
            },
            verticesDeleted: function(data) {
                require(['../util/store'], function(store) {
                    var storeObjects = _.compact(
                            store.getObjects(publicData.currentWorkspaceId, 'vertex', data.vertexIds)
                        );
                    if (storeObjects.length) {
                        require(['../services/vertex'], function(vertex) {
                            vertex.multiple({ vertexIds: _.pluck(storeObjects, 'id') })
                                .then(function(vertices) {
                                    var deleted = _.without(data.vertexIds, _.pluck(vertices, 'id'));
                                    if (deleted.length) {
                                        store.removeWorkspaceVertexIds(publicData.currentWorkspaceId, deleted);
                                        dispatchMain('rebroadcastEvent', {
                                            eventName: 'verticesDeleted',
                                            data: {
                                                vertexIds: data.vertexIds
                                            }
                                        });
                                    }
                                })
                                .catch(function() {
                                    store.removeWorkspaceVertexIds(publicData.currentWorkspaceId, data.vertexIds);
                                    dispatchMain('rebroadcastEvent', {
                                        eventName: 'verticesDeleted',
                                        data: {
                                            vertexIds: data.vertexIds
                                        }
                                    });
                                });
                        });
                    }
                });
            },
            edgeDeletion: function(data) {
                require([
                    '../util/store',
                    '../services/edge'
                ], function(store, edge) {
                    edge.exists([data.edgeId])
                        .then(function(r) {
                            if (!r.exists[data.edgeId]) {
                                store.removeObject(publicData.currentWorkspaceId, 'edge', data.edgeId);
                                dispatchMain('rebroadcastEvent', {
                                    eventName: 'edgesDeleted',
                                    data: data
                                });
                            }
                        })
                });
            },
            textUpdated: function(data) {
                if (data.graphVertexId &&
                    (!data.workspaceId ||
                     data.workspaceId === publicData.currentWorkspaceId)) {

                    dispatchMain('rebroadcastEvent', {
                        eventName: 'textUpdated',
                        data: {
                            vertexId: data.graphVertexId
                        }
                    })
                }
            },
            longRunningProcessChange: function(process) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'longRunningProcessChanged',
                    data: {
                        process: process
                    }
                });
            },
            entityImageUpdated: function(data) {
                if (data && data.graphVertexId) {
                    socketHandlers.propertyChange(data);
                }
            },
            notification: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationActive',
                    data: data
                });
            },
            systemNotificationUpdated: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationUpdated',
                    data: data
                });
            },
            systemNotificationEnded: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'notificationDeleted',
                    data: data
                });
            },
        };

    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        if (messageFromUs(json)) {
            return;
        }

        console.debug('%cSocket: %s %O', 'color:#999;font-style:italics', json.type, json.data)

        if (json.type in socketHandlers) {
            socketHandlers[json.type]('data' in json ? json.data : json, json);
        } else {
            console.warn('Unhandled socket message type:' + json.type, 'message:', json);
        }
    }

    function messageFromUs(json) {
        return json.sourceGuid && json.sourceGuid === publicData.socketSourceGuid;
    }
});
