
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {

        vertexDelete: function(vertexId, workspaceId) {
            return ajax('POST->HTML', '/admin/deleteVertex', {
                graphVertexId: vertexId,
                workspaceId: workspaceId
            });
        },

        dictionary: function() {
            return ajax('GET', '/admin/dictionary');
        },

        dictionaryDelete: function(rowKey) {
            return ajax('POST', '/admin/dictionary/delete', {
                entryRowKey: rowKey
            });
        },

        dictionaryAdd: function(concept, tokens, resolvedName) {
            var data = {
                concept: concept,
                tokens: tokens
            };

            if (resolvedName) {
                data.resolvedName = resolvedName;
            }

            return ajax('POST', '/admin/dictionary', data);
        },

        plugins: function() {
            return ajax('GET', '/admin/plugins');
        },

        systemNotificationCreate: function(options) {
            if ('endDate' in options && !options.endDate) {
                delete options.endDate;
            }
            return ajax('POST', '/notification/system', options);
        },

        systemNotificationList: function() {
            return ajax('GET', '/notification/all');
        },

        systemNotificationDelete: function(id) {
            return ajax('DELETE', '/notification', {
                notificationId: id
            });
        },

        userDelete: function(userName) {
            return ajax('POST', '/user/delete', {
                'user-name': userName
            });
        },

        userUpdatePrivileges: function(userName, privileges) {
            return ajax('POST', '/user/privileges/update', {
                'user-name': userName,
                privileges: _.isArray(privileges) ? privileges.join(',') : privileges
            });
        },

        userAuthAdd: function(userName, auth) {
            return ajax('POST', '/user/auth/add', {
                'user-name': userName,
                auth: auth
            });
        },

        userAuthRemove: function(userName, auth) {
            return ajax('POST', '/user/auth/remove', {
                'user-name': userName,
                auth: auth
            });
        },

        workspaceShare: function(workspaceId, userName) {
            return ajax('POST', '/workspace/shareWithMe', {
                'user-name': userName,
                workspaceId: workspaceId
            });
        },

        workspaceImport: function(workspaceFile) {
            var formData = new FormData();
            formData.append('workspace', workspaceFile);
            return ajax('POST->HTML', '/admin/workspace/import', formData);
        },

        queueVertices: function(propertyName) {
            var data = {};
            if (propertyName) {
                data.propertyName = propertyName;
            }
            return ajax('POST->HTML', '/admin/queueVertices', data);
        },

        queueEdges: function(propertyName) {
            return ajax('POST->HTML', '/admin/queueEdges');
        },

        ontologyUpload: function(iri, file) {
            var formData = new FormData();
            formData.append('file', file);
            formData.append('documentIRI', iri);
            return ajax('POST->HTML', '/admin/upload-ontology', formData);
        },

        ontologyEdit: function(options) {
            return ajax('POST->HTML', '/admin/saveOntologyConcept', options);
        }

    };

    return api;
});
