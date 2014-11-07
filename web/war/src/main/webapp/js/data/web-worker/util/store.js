
define([
    'jscache'
], function(Cache) {
    'use strict';

        // Object cache per workspace
    var workspaceCaches = {},

        // Need all vertices in current workspace somehow
        workspaceVertices = {},

        api = {
            checkAjaxForPossibleCaching: function(xhr, json, workspaceId, request) {
                if (resemblesVertices(json.vertices)) {
                    var vertexCache = cacheForWorkspace(workspaceId).vertices;

                    json.vertices.forEach(function(v) {
                        vertexCache.setItem(v.id, v, {
                            expirationAbsolute: null,
                            expirationSliding: 60,
                            priority: Cache.Priority.HIGH,
                            callback: function(k, v) {
                                console.debug('removed ' + k);
                            }
                        });
                    });

                    console.log(
                        vertexCache
                    );

                }
            }
        };

    return api;

    function cacheForWorkspace(workspaceId) {
        if (workspaceId in workspaceCaches) {
            return workspaceCaches[workspaceId];
        }

        workspaceCaches[workspaceId] = {
            vertices: new Cache(),
            edges: new Cache()
        };

        return workspaceCaches[workspaceId];
    }

    function resemblesEdges(val) {
    }

    function resemblesVertices(val) {
        return val && val.length && val[0].id && val[0].properties;
    }
});
