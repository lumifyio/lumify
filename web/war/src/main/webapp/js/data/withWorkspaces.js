define([], function() {
    'use strict';

    return withWorkspaces;

    function withWorkspaces() {

        this.after('initialize', function() {
            this.on('loadCurrentWorkspace', this.onLoadCurrentWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
        });

        this.onLoadCurrentWorkspace = function(event) {
            var currentWorkspaceId = this.lumifyData.currentWorkspaceId;
            this.trigger('switchWorkspace', { workspaceId: currentWorkspaceId });
        }

        this.onSwitchWorkspace = function(event, data) {
            this.setPublicApi('currentWorkspaceId', data.workspaceId);
            this.setPublicApi('currentWorkspaceEditable', data.editable);
            this.worker.postMessage({
                type: 'switchWorkspace',
                workspaceId: data.workspaceId
            });
        };

        this.edgesLoaded = function(message) {
            this.trigger('edgesLoaded', message);
        };

        this.workspaceLoaded = function(message) {
            var workspace = message.workspace;
            workspace.data = {
                vertices: message.vertices
            };
            this.trigger('workspaceLoaded', workspace);
        };
    }
});
