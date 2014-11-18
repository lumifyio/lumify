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
            userStatusChange: function(data) {
                dispatchMain('rebroadcastEvent', {
                    eventName: 'userStatusChange',
                    data: data
                });
            },
            userWorkspaceChange: NOOP,
        //'edgeDeletion',
        //'entityImageUpdated',
        //'longRunningProcessChange',
        //'propertiesChange',
        //'sync',
        //'textUpdated',
        //'userWorkspaceChange',
        //'verticesDeleted',
        //'workspaceChange',
        //'workspaceDelete',
    };

    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        if (messageFromUs(json)) {
            return;
        }

        console.info(json.type, json.data);

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
