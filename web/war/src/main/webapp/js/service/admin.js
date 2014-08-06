
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

    return AdminService;
});
