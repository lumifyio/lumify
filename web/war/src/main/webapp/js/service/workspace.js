
define(
[
    'service/serviceBase'
],
function(ServiceBase) {
    'use strict';

    function WorkspaceService() {
        ServiceBase.call(this);

        return this;
    }

    WorkspaceService.prototype = Object.create(ServiceBase.prototype);

    WorkspaceService.prototype.list = function() {
        return this._ajaxGet({
            url: 'workspace/all'
        });
    };

    WorkspaceService.prototype.getByRowKey = function(workspaceId) {
        return this._ajaxGet({
            url: 'workspace',
            data: {
                workspaceId: workspaceId
            }
        });
    };

    WorkspaceService.prototype.getVertices = function(workspaceId) {
        return this._ajaxGet({
            url: 'workspace/vertices',
            data: {
                workspaceId: workspaceId
            }
        });
    };

    WorkspaceService.prototype.getEdges = function(workspaceId, additionalIds) {
        var data = {},
            method = '_ajaxGet';

        if (additionalIds && additionalIds.length) {
            data.ids = additionalIds;
            if (additionalIds.length > 10) {
                method = '_ajaxPost';
            }
        }
        data.workspaceId = workspaceId;

        return this[method]({
            url: 'workspace/edges',
            data: data
        });
    };

    WorkspaceService.prototype.create = function(title) {
        return this._ajaxPost({
            url: 'workspace/create',
            data: {
                title: title
            }
        });
    };

    WorkspaceService.prototype.diff = function(workspaceId) {
        return this._ajaxGet({
            url: 'workspace/diff',
            data: {
                workspaceId: workspaceId
            }
        })
    };

    WorkspaceService.prototype.publish = function(changes) {
        return this._ajaxPost({
            url: 'workspace/publish',
            data: {
                publishData: JSON.stringify(changes)
            }
        })
    };

    WorkspaceService.prototype.undo = function(changes) {
        return this._ajaxPost({
            url: 'workspace/undo',
            data: {
                undoData: JSON.stringify(changes)
            }
        })
    };

    WorkspaceService.prototype.save = function(workspaceId, changes) {
        var options = {
            url: 'workspace/update',
            data: {
                workspaceId: workspaceId,
                data: {
                    entityUpdates: [],
                    entityDeletes: [],
                    userUpdates: [],
                    userDeletes: []
                }
            }
        };

        if (_.isEmpty(changes)) {
            console.warn('Workspace update called with no changes');
        }

        if (changes) {
            options.data.data = JSON.stringify($.extend(options.data.data, changes));
        }
        return this._ajaxPost(options);
    };

    WorkspaceService.prototype.copy = function(workspaceId) {
        var options = {
            url: 'workspace/copy',
            data: {
                workspaceId: workspaceId
            }
        }

        return this._ajaxPost(options);
    }

    WorkspaceService.prototype['delete'] = function(workspaceId) {
        return this._ajaxDelete({
            url: 'workspace?' + $.param({ workspaceId: workspaceId })
        });
    };

    return WorkspaceService;
});
