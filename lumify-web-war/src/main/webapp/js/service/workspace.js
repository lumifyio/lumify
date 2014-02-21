
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

    WorkspaceService.prototype.save = function (workspaceId, changes) {
        var options = {
            url: workspaceId === null ?
                    'workspace/new' :
                    'workspace/' + encodeURIComponent(workspaceId) + '/update',
            data: {}
        };
        
        if (changes) {
            if (changes.title) {
                options.data.title = changes.title;
                delete changes.title;
            }
            options.data.data = JSON.stringify($.extend({
                entityUpdates: [], entityDeletes: [], userUpdates: [], userDeletes: []
            }, changes));
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
            url: 'workspace/' + encodeURIComponent(workspaceId)
        });
    };

    return WorkspaceService;
});

