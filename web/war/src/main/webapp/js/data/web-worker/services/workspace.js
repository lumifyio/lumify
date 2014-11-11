
define(['../util/ajax'], function(ajax) {
    'use strict';

    return {
        diff: function(workspaceId) {
            return ajax('GET', '/workspace/diff', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            });
        },

        all: function() {
            return ajax('GET', '/workspace/all');
        },

        get: function(workspaceId) {
            return ajax('GET', '/workspace', {
                workspaceId: workspaceId
            }).then(function(workspace) {
                workspace.vertices = _.indexBy(workspace.vertices, 'vertexId');
                return workspace;
            });
        },

        vertices: function(workspaceId) {
            return ajax('GET', '/workspace/vertices', {
                workspaceId: workspaceId
            });
        },

        create: function(options) {
            return ajax('POST', '/workspace/create', options);
        }
    }
})
