define(
    [
        'service/serviceBase'
    ],
    function (ServiceBase) {
        'use strict';

        function SyncService() {
            ServiceBase.call(this);
            return this;
        }

        SyncService.prototype = Object.create(ServiceBase.prototype);

        SyncService.prototype.publishWorkspaceSyncEvent = function (eventName, workspaceRowKey, eventData) {
            var data = {
                type: 'sync',
                permissions: {
                    workspaces: [workspaceRowKey]
                },
                data: {
                    eventName: eventName,
                    eventData: eventData
                }
            };

            this.socketPush(data);
            return null;
        };

        SyncService.prototype.publishWorkspaceMetadataSyncEvent = function (eventName, workspaceRowKey, eventData) {
            // Store previous users so we send that last sync event to the user
            // that was "Revoked"
            if (!this._previousUsers) this._previousUsers = [];
            var users = _.uniq(_.pluck(eventData.users,'user').concat([eventData.createdBy]).concat(this._previousUsers));
            this._previousUsers = users;

            var data = {
                type: 'sync',
                permissions: {
                    users: users
                },
                data: {
                    eventName: eventName,
                    eventData: eventData
                }
            };

            this.socketPush(data);
        };

        SyncService.prototype.publishUserSyncEvent = function (eventName, userIds, eventData) {
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

