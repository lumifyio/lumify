
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

    AdminService.prototype.ontologyUpload = function(iri, file) {
        var formData = new FormData();
        formData.append('file', file);
        formData.append('documentIRI', iri);

        return this._ajaxUpload({
            url: 'admin/uploadOntology',
            dataType: 'html',
            data: formData
        });
    };

    AdminService.prototype.ontologyEdit = function(concept) {
        return this._ajaxPost({
            url: 'admin/saveOntologyConcept',
            dataType: 'html',
            data: concept
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

    AdminService.prototype.plugins = function() {
        return this._ajaxGet({
            url: 'admin/plugins'
        });
    };

    AdminService.prototype.dictionary = function() {
        return this._ajaxGet({
            url: 'admin/dictionary'
        });
    };

    AdminService.prototype.dictionaryAdd = function(concept, tokens, resolvedName) {
        var data = {
            concept: concept,
            tokens: tokens
        };

        if (resolvedName) {
            data.resolvedName = resolvedName;
        }

        return this._ajaxPost({
            url: 'admin/dictionary',
            data: data
        });
    };

    AdminService.prototype.dictionaryDelete = function(rowKey) {
        return this._ajaxPost({
            url: 'admin/dictionary/delete',
            data: {
                entryRowKey: rowKey
            }
        });
    };

    AdminService.prototype.userAuthAdd = function(userName, auth) {
        return this._ajaxPost({
            url: 'user/auth/add',
            data: {
                'user-name': userName,
                auth: auth
            }
        });
    };

    AdminService.prototype.userAuthRemove = function(userName, auth) {
        return this._ajaxPost({
            url: 'user/auth/remove',
            data: {
                'user-name': userName,
                auth: auth
            }
        });
    };

    AdminService.prototype.userUpdatePrivileges = function(userName, privileges) {
        return this._ajaxPost({
            url: 'user/privileges/update',
            data: {
                'user-name': userName,
                privileges: _.isArray(privileges) ? privileges.join(',') : privileges
            }
        });
    };  
    
    AdminService.prototype.userDelete = function(userName) {
        return this._ajaxPost({
            url: 'user/delete',
            data: {
                'user-name': userName
            }
        });
    };

    AdminService.prototype.workspaceShare = function(userName, workspaceId) {
        return this._ajaxPost({
            url: 'workspace/shareWithMe',
            data: {
                'user-name': userName,
                workspaceId: workspaceId
            }
        });
    };

    return AdminService;
});
