
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
            data: formData
        });
    };

    return AdminService;
});
