
define([
    'flight/lib/registry'
], function(registry) {
    'use strict';

    // TODO: make this work? or delete

    return withWorkspaceData;

    function withWorkspaceData() {

        this.getApplicationInstance = function() {
            var instanceInfo = registry.findInstanceInfoByNode($('#app')[0]);

            return instanceInfo[0].instance;
        };

        this.getWorkspaceVertices = function() {
            var instance = this.getApplicationInstance();

            if (instance.workspaceData &&
                instance.workspaceData.data &&
                instance.workspaceData.data.vertices) {
                return instance.workspaceData.data.vertices;
            }

            return [];
        };
    }
});
