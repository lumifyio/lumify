define(['require'], function(require) {
    'use strict';

    var NOOP = function() {},
        socketHandlers = {
            workspaceChange: function(data) {
                require(['../util/store'], function(store) {
                    store.workspaceWasChangedRemotely(data);
                })
            },
            workspaceDelete: function(data) {
                require(['../util/store'], function(store) {
                    store.removeWorkspace(data.workspaceId);
                    dispatchMain('rebroadcastEvent', {
                        eventName: 'workspaceDeleted',
                        data: {
                            workspaceId: data.workspaceId
                        }
                    })
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
            propertiesChange: function(data) {
                require(['../util/store'], function(store) {
                    store.updateObject(data, { onlyIfExists:true });
                });
            },
            edgeDeletion: function(data) {
                if (!data.workspaceId || data.workspaceId === publicData.currentWorkspaceId) {
                    dispatchMain('rebroadcastEvent', {
                        eventName: 'edgesDeleted',
                        data: {
                            edgeId: data.edgeId,
                            sourceVertexId: data.inVertexId,
                            destVertexId: data.outVertexId
                        }
                    });
                }
                require(['../util/store'], function(store) {
                    store.removeObject(data.workspaceId, 'edge', data.edgeId);
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
                    require(['../util/store'], function(store) {
                        store.updateObject(data, { onlyIfExists:true });
                    });

                }
            }
        };

    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        if (messageFromUs(json)) {
            return;
        }

        console.debug('%cSocket: %s %O', 'color:#999;font-style:italics', json.type, json.data)

        if (json.type in socketHandlers) {
            socketHandlers[json.type](json.data || json);
        } else {
            console.warn('Unhandled socket message type:' + json.type, 'message:', json);
        }
    }

    function messageFromUs(json) {
        return json.sourceGuid && json.sourceGuid === publicData.socketSourceGuid;
    }
});
