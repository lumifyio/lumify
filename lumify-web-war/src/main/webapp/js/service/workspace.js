
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

    WorkspaceService.prototype.saveNew = function(title) {
        return this._ajaxPost({
            url: 'workspace/new',
            data: {
                title: title
            }
        });
    };

    WorkspaceService.prototype.diff = function() {
        return this._ajaxGet({
            url: 'workspace/diff'
        })
    };

    WorkspaceService.prototype.save = function (workspaceId, changes) {
        var options = {
            url: 'workspace/' + encodeURIComponent(workspaceId) + '/update',
            data: {
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

