define(
    [
        'service/serviceBase'
    ],
    function(ServiceBase) {
        'use strict';

        function SyncService() {
            ServiceBase.call(this);
            return this;
        }

        SyncService.prototype = Object.create(ServiceBase.prototype);

        SyncService.prototype.publishWorkspaceSyncEvent = function(eventName, workspaceId, eventData) {
            var data = {
                type: 'sync',
                permissions: {
                    workspaces: [workspaceId]
                },
                data: {
                    eventName: eventName,
                    eventData: eventData
                }
            };

            this.socketPush(data);
            return null;
        };

        SyncService.prototype.publishUserSyncEvent = function(eventName, userIds, eventData) {
            var data = {
                type: 'sync',
                permissions: {
                    users: userIds
                },
                data: {
                    eventName: eventName,
                    eventData: eventData
                }
            };

            this.socketPush(data);
            return null;
        };

        return SyncService;
    }
);
