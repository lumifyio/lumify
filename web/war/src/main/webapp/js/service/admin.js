
define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    function AdminService() {
        ServiceBase.call(this);
        return this;
    }

    AdminService.prototype = Object.create(ServiceBase.prototype);

    AdminService.prototype.workspaceImport = function(workspaceFile) {
        var formData = new FormData();
        formData.append('workspace', workspaceFile);
        return this._ajaxUpload({
            url: 'admin/workspace/import',
            dataType: 'html',
            data: formData
        });
    };

    AdminService.prototype.queueVertices = function(parameterName) {
        var data = {};
        if (parameterName) {
            data.parameterName = parameterName;
        }
        return this._ajaxPost({
            url: 'admin/queueVertices',
            dataType: 'html',
            data: data
        });
    };

    AdminService.prototype.queueEdges = function() {
        return this._ajaxPost({
            url: 'admin/queueEdges',
            dataType: 'html'
        });
    };

    return AdminService;
});
