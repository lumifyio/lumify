define(['../services/workspace'], function(Workspace) {
    'use strict';

    return function(message) {
        Promise.all([
            Workspace.get(message.workspaceId),
            Workspace.vertices(message.workspaceId),
            Promise.require('data/web-worker/util/cache')
        ]).done(function(results) {
            var workspace = results[0],
                vertices = results[1].vertices,
                cache = results[2];

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
            //var changes = cache.cacheAjaxResult(jsonResponse, url, workspaceId);
        });
    }
});
