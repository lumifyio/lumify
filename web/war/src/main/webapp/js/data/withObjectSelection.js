define([
    'util/clipboardManager'
], function(ClipboardManager) {
    'use strict';

    return withObjectSelection;

    function defaultNoObjectsOrData(data) {
        var baseObj = {
                vertices: [],
                edges: [],
                vertexIds: {},
                edgeIds: {}
            },
            returnable = _.extend({}, baseObj, data || {});

        returnable.vertexIds = _.indexBy(returnable.vertices, 'id');
        returnable.edgeIds = _.indexBy(returnable.edges, 'id');

        return returnable;
    }

    function withObjectSelection() {

        var selectedObjects,
            previousSelectedObjects;

        this.after('initialize', function() {
            ClipboardManager.attachTo(this.$node);

            this.setPublicApi('selectedObjects', defaultNoObjectsOrData());

            this.on('selectObjects', this.onSelectObjects);
            this.on('selectAll', this.onSelectAll);

            this.on('deleteSelected', this.onDeleteSelected);
            this.on('deleteEdges', this.onDeleteEdges);
            this.on('edgesDeleted', function(event, data) {
                if (selectedObjects && _.findWhere(selectedObjects.edges, { id: data.edgeId })) {
                    this.trigger('selectObjects');
                }
            })

            this.on('searchTitle', this.onSearchTitle);
            this.on('searchRelated', this.onSearchRelated);
            this.on('addRelatedItems', this.onAddRelatedItems);
            this.on('objectsSelected', this.onObjectsSelected);
        });

        this.onSelectAll = function(event, data) {
            var self = this;

            this.dataRequestPromise.done(function(dataRequest) {
                dataRequest('workspace', 'store')
                    .done(function(vertices) {
                        self.trigger('selectObjects', { vertexIds: _.keys(vertices) });
                    })
            })
        };

        this.onDeleteSelected = function(event, data) {
            var self = this;

            require(['util/privileges'], function(Privileges) {
                if (!Privileges.canEDIT) {
                    return;
                }

                if (data && data.vertexId) {
                    self.trigger('updateWorkspace', {
                        entityDeletes: [data.vertexId]
                    });
                } else if (selectedObjects) {
                    if (selectedObjects.vertices.length) {
                        self.trigger('updateWorkspace', {
                            entityDeletes: _.pluck(selectedObjects.vertices, 'id')
                        });
                    } else if (selectedObjects.edges.length) {
                        self.trigger('deleteEdges', { edges: selectedObjects.edges });
                    }
                }
            });
        };

        this.onDeleteEdges = function(event, data) {
            var edge = data && data.edges && data.edges.length === 1 && data.edges[0];

            if (edge) {
                this.dataRequestPromise.done(function(dataRequest) {
                    dataRequest('edge', 'delete',
                        edge.id,
                        edge.source.id,
                        edge.target.id
                    );
                });
            } else console.error('Only can delete one edge at a time');
        };

        this.onSelectObjects = function(event, data) {
            var self = this,
                promises = [];

            this.dataRequestPromise.done(function(dataRequest) {
                if (data && data.vertexIds) {
                    if (!_.isArray(data.vertexIds)) {
                        data.vertexIds = [data.vertexIds];
                    }
                    promises.push(
                        dataRequest('vertex', 'store', { vertexIds: data.vertexIds })
                    );
                } else if (data && data.vertices) {
                    promises.push(Promise.resolve(data.vertices));
                }

                if (data && data.edgeIds && data.edgeIds.length) {
                    promises.push(
                        dataRequest('edge', 'store', { edgeIds: data.edgeIds.slice(0, 1) })
                    );
                } else if (data && data.edges) {
                    promises.push(Promise.resolve(data.edges));
                }

                Promise.all(promises)
                    .done(function(result) {
                        var vertices = result[0] || [],
                            edges = result[1] || [];

                        selectedObjects = {
                            vertices: vertices,
                            edges: vertices.length ? [] : edges
                        };

                        if (previousSelectedObjects &&
                            selectedObjects &&
                            _.isEqual(previousSelectedObjects, selectedObjects)) {
                            return;
                        }

                        previousSelectedObjects = selectedObjects;

                        self.setPublicApi('selectedObjects', defaultNoObjectsOrData(selectedObjects));

                        var postData = _.clone(selectedObjects);
                        if (data && 'focus' in data) {
                            postData.focus = data.focus;
                        }
                        self.trigger('objectsSelected', postData);
                    });
            });
        };

        this.onObjectsSelected = function(event, data) {
            var self = this;

            if (data.vertices.length) {
                require(['util/vertex/urlFormatters'], function(F) {
                    self.trigger('clipboardSet', {
                        text: F.vertexUrl.url(data.vertices, lumifyData.currentWorkspaceId)
                    });
                })
            } else {
                this.trigger('clipboardClear');
            }

            if (window.DEBUG) {
                DEBUG.selectedObjects = data;
            }
        };

        this.onSearchTitle = function(event, data) {
            var self = this,
                vertexId = data.vertexId || (
                    selectedObjects &&
                    selectedObjects.vertices.length === 1 &&
                    selectedObjects.vertices[0].id
                );

            if (vertexId) {
                Promise.all([
                    Promise.require('util/vertex/formatters'),
                    this.dataRequestPromise.then(function(dataRequest) {
                        return dataRequest('vertex', 'store', { vertexIds: vertexId });
                    })
                ]).done(function(results) {
                    var F = results.shift(),
                        vertex = results.shift(),
                        title = F.vertex.title(vertex);

                    self.trigger('searchByEntity', { query: title });
                })
            }
        };

        this.onSearchRelated = function(event, data) {
            var vertexId = data.vertexId || (
                selectedObjects &&
                selectedObjects.vertices.length === 1 &&
                selectedObjects.vertices[0].id
            );

            if (vertexId) {
                this.trigger('searchByRelatedEntity', { vertexId: vertexId });
            }
        };

        this.onAddRelatedItems = function(event, data) {
            var self = this;

            if (!data || _.isUndefined(data.vertexId)) {
                if (selectedObjects && selectedObjects.vertices.length === 1) {
                    data = {
                        vertexId: selectedObjects.vertices[0].id
                    };
                } else {
                    return;
                }
            }

            Promise.all([
                Promise.require('util/popovers/addRelated/addRelated'),
                Promise.require('util/vertex/formatters'),
                this.dataRequestPromise.then(function(dataRequest) {
                    return dataRequest('vertex', 'store', { vertexIds: data.vertexId })
                })
            ]).done(function(results) {
                var RP = results.shift(),
                    F = results.shift(),
                    vertex = results.shift();

                RP.teardownAll();

                RP.attachTo(event.target, {
                    vertex: vertex,
                    relatedToVertexId: data.vertexId,
                    anchorTo: {
                        vertexId: data.vertexId
                    }
                });
            });
        };
    }
});
