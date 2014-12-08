
define([
    '../util/ajax',
    '../util/store',
    '../util/abort'
], function(ajax, store, abortPrevious) {
    'use strict';

    return {
        diff: function(workspaceId) {
            return ajax('GET', '/workspace/diff', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            });
        },

        all: function() {
            return ajax('GET', '/workspace/all')
                .then(function(result) {
                    return _.sortBy(result.workspaces, function(w) {
                        return w.title.toLowerCase();
                    });
                })
        },

        get: function(workspaceId) {
            return ajax('GET', '/workspace', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            }).then(function(workspace) {
                return store.setWorkspace(workspace);
            });
        },

        'delete': function(workspaceId) {
            return ajax('DELETE', '/workspace', {
                workspaceId: workspaceId
            });
        },

        store: function(workspaceId) {
            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace');
            return Promise.resolve(workspace && workspace.vertices || []);
        },

        histogramValues: function(workspaceId, property) {
            if (arguments.length === 1) {
                property = arguments[0];
                workspaceId = null;
            }

            var workspace = store.getObject(workspaceId || publicData.currentWorkspaceId, 'workspace'),
                vertexIds = _.keys(workspace.vertices),
                vertices = store.getObjects(workspace.workspaceId, 'vertex', vertexIds),
                values = _.chain(vertices)
                    .map(function(v) {
                        var properties = _.where(v.properties, { name: property.title });
                        return _.pluck(properties, 'value');
                    })
                    .flatten(true)
                    .compact()
                    .value();

            return Promise.resolve(values);
        },

        save: abortPrevious(function(workspaceId, changes) {
            if (arguments.length === 1) {
                changes = workspaceId;
                workspaceId = publicData.currentWorkspaceId;
            }

            var workspace = store.getObject(workspaceId, 'workspace');

            if (_.isEmpty(changes)) {
                console.warn('Workspace update called with no changes');
                return Promise.resolve({ saved: false, workspace: workspace });
            }

            var allChanges = _.extend({}, {
                entityUpdates: [],
                entityDeletes: [],
                userUpdates: [],
                userDeletes: []
            }, changes || {});

            allChanges.entityUpdates.forEach(function(entityUpdate) {
                var p = entityUpdate.graphPosition,
                    layout = entityUpdate.graphLayoutJson;

                if (p) {
                    p.x = Math.round(p.x);
                    p.y = Math.round(p.y);
                }
                if (layout) {
                    entityUpdate.graphLayoutJson = JSON.stringify(layout);
                } else if (!p) {
                    console.error('Entity updates require either graphPosition or graphLayoutJson', entityUpdate);
                }
            })

            if (!store.workspaceShouldSave(workspace, allChanges)) {
                return Promise.resolve({ saved: false, workspace: workspace });
            }

            return ajax('POST', '/workspace/update', {
                workspaceId: workspaceId,
                data: JSON.stringify(allChanges)
            }).then(function() {
                return { saved: true, workspace: store.updateWorkspace(workspaceId, allChanges) };
            })
        }),

        vertices: function(workspaceId) {
            return ajax('GET', '/workspace/vertices', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            });
        },

        publish: function(changes) {
            return ajax('POST', '/workspace/publish', {
                publishData: JSON.stringify(changes)
            });
        },

        undo: function(changes) {
            return ajax('POST', '/workspace/undo', {
                undoData: JSON.stringify(changes)
            });
        },

        edges: function(workspaceId, additionalVertices) {
            return ajax('GET', '/workspace/edges', {
                workspaceId: workspaceId || publicData.currentWorkspaceId
            }).then(function(result) {
                return result.edges;
            })
        },

        create: function(options) {
            return ajax('POST', '/workspace/create', options);
        }
    }
})
