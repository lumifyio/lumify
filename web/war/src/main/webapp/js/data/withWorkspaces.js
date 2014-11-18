define([], function() {
    'use strict';

    return withWorkspaces;

    function withWorkspaces() {
        var lastReloadedState;

        this.after('initialize', function() {
            this.fireApplicationReadyOnce = _.once(this.trigger.bind(this, 'applicationReady'));

            this.on('loadCurrentWorkspace', this.onLoadCurrentWorkspace);
            this.on('switchWorkspace', this.onSwitchWorkspace);
            this.on('reloadWorkspace', this.onReloadWorkspace);
            this.on('updateWorkspace', this.onUpdateWorkspace);
            this.on('loadEdges', this.onLoadEdges);
        });

        this.onLoadCurrentWorkspace = function(event) {
            var currentWorkspaceId = this.lumifyData.currentWorkspaceId;
            this.trigger('switchWorkspace', { workspaceId: currentWorkspaceId });
        };

        this.onReloadWorkspace = function() {
            if (lastReloadedState) {
                this.workspaceLoaded(lastReloadedState.workspace);
                this.edgesLoaded(lastReloadedState.edges);
            }
        };

        this.onLoadEdges = function(event, data) {
            var self = this;
            this.dataRequest('workspace', 'edges', data && data.workspaceId)
                .done(function(edges) {
                    self.edgesLoaded({ edges:edges });
                })
        };

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
            var self = this,
                triggered = false,
                buffer = _.delay(function() {
                    triggered = true;
                    self.trigger('workspaceSaving', lastReloadedState.workspace);
                }, 250)

            this.dataRequest('workspace', 'save', data)
                .then(function(data) {
                    clearTimeout(buffer);
                    if (data.saved) {
                        self.trigger('workspaceSaved', lastReloadedState.workspace);
                    }
                })
                .finally(function() {
                    if (triggered) {
                        self.trigger('workspaceSaved', lastReloadedState.workspace);
                    }
                })
                .done();
        };

        // Worker Handlers

        this.edgesLoaded = function(message) {
            lastReloadedState.edges = message;
            this.trigger('edgesLoaded', message);
        };

        this.workspaceUpdated = function(message) {
            if (lastReloadedState &&
                lastReloadedState.workspace.workspaceId === message.workspace.workspaceId) {
                lastReloadedState.workspace = message.workspace;
            }
            this.trigger('workspaceUpdated', message);
            if (message.newVertices.length) {
                this.trigger('loadEdges', {
                    workspaceId: message.workspace.workspaceId
                });
            }
        };

        this.workspaceLoaded = function(message) {
            lastReloadedState.workspace = message;
            var workspace = message.workspace;
            workspace.data = {
                vertices: message.vertices
            };
            this.trigger('workspaceLoaded', workspace);
            this.fireApplicationReadyOnce();
        };
    }
});
