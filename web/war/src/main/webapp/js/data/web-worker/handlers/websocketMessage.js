define(['require'], function(require) {
    'use strict';

    var socketHandlers = {
        workspaceChange: function(data) {
            require(['../util/store'], function(store) {
                store.workspaceWasChangedRemotely(data);
            })
        }
    };

    return function(data) {
        var body = data.responseBody,
            json = JSON.parse(body);

        if (messageFromUs(json)) {
            return;
        }

        console.info('socketmessage', json);

        if (json.type in socketHandlers) {
            socketHandlers[json.type](json.data || json);
        }
    }

    function messageFromUs(json) {
        return json.sourceGuid && json.sourceGuid === publicData.socketSourceGuid;
    }
});
