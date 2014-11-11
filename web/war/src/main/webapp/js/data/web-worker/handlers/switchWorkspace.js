define(['../services/workspace'], function(Workspace) {
    'use strict';

    return function(message) {
        Promise.all([
            Workspace.get(message.workspaceId),
            Workspace.vertices(message.workspaceId),
            Promise.require('data/web-worker/util/store')
        ]).done(function(results) {
            var workspace = results[0],
                vertices = results[1].vertices,
                store = results[2];

            pushSocketMessage({
                type: 'setActiveWorkspace',
                data: {
                    workspaceId: workspace.workspaceId,
                    userId: publicData.currentUser.id
                }
            });
            dispatchMain('workspaceLoaded', {
                workspace: workspace,
                vertices: vertices
            });

            store.setVerticesInWorkspace(workspace.workspaceId, _.pluck(workspace.vertices, 'vertexId'));
        });
    }
});
