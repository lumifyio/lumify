define([], function() {
    'use strict';

    return withWorkspaces;

    function withWorkspaces() {
        var lastReloadedState;

        this.after('initialize', function() {
            this.on('loadCurrentWorkspace', this.onLoadCurrentWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
            this.on('reloadWorkspace', this.onReloadWorkspace);
            this.on('updateWorkspace', this.onUpdateWorkspace);
        });

        this.onLoadCurrentWorkspace = function(event) {
            var currentWorkspaceId = this.lumifyData.currentWorkspaceId;
            this.trigger('switchWorkspace', { workspaceId: currentWorkspaceId });
        }

        this.onReloadWorkspace = function() {
            if (lastReloadedState) {
                this.workspaceLoaded(lastReloadedState.workspace);
                this.edgesLoaded(lastReloadedState.edges);
            }
        }

        this.onSwitchWorkspace = function(event, data) {
            lastReloadedState = {};
            this.setPublicApi('currentWorkspaceId', data.workspaceId);
            this.setPublicApi('currentWorkspaceEditable', data.editable);
            this.worker.postMessage({
                type: 'workspaceSwitch',
                workspaceId: data.workspaceId
            });
        };

        this.onUpdateWorkspace = function(event, data) {
            var self = this;

            this.trigger('workspaceSaving', lastReloadedState.workspace);
            this.dataRequest('workspace', 'save', data)
                .finally(function() {
                    self.trigger('workspaceSaved', lastReloadedState.workspace);
                })
                .done();
        };

        // Worker Handlers

        this.edgesLoaded = function(message) {
            lastReloadedState.edges = message;
            this.trigger('edgesLoaded', message);
        };

        this.workspaceLoaded = function(message) {
            lastReloadedState.workspace = message;
            var workspace = message.workspace;
            workspace.data = {
                vertices: message.vertices
            };
            this.trigger('workspaceLoaded', workspace);
        };
    }
});
