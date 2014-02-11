
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

    WorkspaceService.prototype.getByRowKey = function (_rowKey) {
        return this._ajaxGet({
            url: 'workspace/' + encodeURIComponent(_rowKey)
        });
    };

    WorkspaceService.prototype.saveNew = function (workspace) {
        return this.save(null, workspace);
    };

    WorkspaceService.prototype.save = function (_rowKey, workspace) {
        var options = {
            url: _rowKey === null ? 
                    'workspace/save' :
                    'workspace/' + encodeURIComponent(_rowKey) + '/save',
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

    WorkspaceService.prototype.copy = function (_rowKey) {
        var options = {
            url: 'workspace/' + _rowKey + '/copy'
        }

        return this._ajaxPost(options);
    }

    WorkspaceService.prototype['delete'] = function(_rowKey) {
        return this._ajaxDelete({
            url: 'workspace/' + encodeURIComponent(_rowKey),
            data: {
                _rowKey: _rowKey
            }
        });
    };

    return WorkspaceService;
});

