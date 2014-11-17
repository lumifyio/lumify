
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

        plugins: function() {
            return ajax('GET', '/admin/plugins');
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
        }

    };

    return api;
});
