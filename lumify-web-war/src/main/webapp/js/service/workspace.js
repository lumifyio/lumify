
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
            url: 'workspace'
        });
    };

    WorkspaceService.prototype.getByRowKey = function (workspaceId) {
        return this._ajaxGet({
            url: 'workspace/' + encodeURIComponent(workspaceId)
        });
    };

    WorkspaceService.prototype.saveNew = function (workspace) {
        return this.save(null, workspace);
    };

    WorkspaceService.prototype.save = function (workspaceId, workspace) {
        var options = {
            url: workspaceId === null ?
                    'workspace/save' :
                    'workspace/' + encodeURIComponent(workspaceId) + '/save',
            data: {}
        };
        
        if (workspace.data) {
            options.data.data = JSON.stringify(workspace.data);
        }
        if (workspace.title) {
            options.data.title = workspace.title;
        }
        if (workspace.users) {
            options.data.users = JSON.stringify(workspace.users);
        }
        return this._ajaxPost(options);
    };

    WorkspaceService.prototype.copy = function (workspaceId) {
        var options = {
            url: 'workspace/' + workspaceId + '/copy'
        }

        return this._ajaxPost(options);
    }

    WorkspaceService.prototype['delete'] = function(workspaceId) {
        return this._ajaxDelete({
            url: 'workspace/' + encodeURIComponent(workspaceId),
            data: {
                workspaceId: workspaceId
            }
        });
    };

    return WorkspaceService;
});

