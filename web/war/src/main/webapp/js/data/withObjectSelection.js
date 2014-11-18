define([
    'util/clipboardManager'
], function(ClipboardManager) {
    'use strict';

    return withObjectSelection;

    function defaultNoObjectsOrData(data) {
        var baseObj = {
                vertices: [],
                edges: [],
                vertexIds: [],
                edgeIds: []
            },
            returnable = _.extend({}, baseObj);

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

            this.on('searchTitle', this.onSearchTitle);
            this.on('searchRelated', this.onSearchRelated);
            this.on('addRelatedItems', this.onAddRelatedItems);
            this.on('objectsSelected', this.onObjectsSelected);
        });

        this.onSelectAll = function(event, data) {
            var self = this;

            this.dataRequest('workspace', 'store')
                .done(function(vertices) {
                    self.trigger('selectObjects', { vertexIds: _.keys(vertices) });
                })
        };

        this.onDeleteSelected = function(event, data) {
            if (data && data.vertexId) {
                this.trigger('updateWorkspace', {
                    entityDeletes: [data.vertexId]
                });
            } else if (selectedObjects) {
                if (selectedObjects.vertices) {
                    this.trigger('updateWorkspace', {
                        entityDeletes: _.pluck(selectedObjects.vertices, 'id')
                    });
                } else if (selectedObjects.edges && Privileges.canEDIT) {
                    this.trigger('deleteEdges', { edges: selectedObjects.edges });
                }
            }
        };

        this.onSelectObjects = function(event, data) {
            var self = this,
                promises = [];

            if (data && data.vertexIds) {
                if (!_.isArray(data.vertexIds)) {
                    data.vertexIds = [data.vertexIds];
                }
                promises.push(
                    this.dataRequest('vertex', 'store', { vertexIds: data.vertexIds })
                );
            } else if (data && data.vertices) {
                promises.push(Promise.resolve(data.vertices));
            }

            if (data && data.edgeIds && data.edgeIds.length) {
                // Only supports one
                if (_.isArray(data.edgeIds)) {
                    data.edgeIds = data.edgeIds[0];
                }
                promises.push(
                    this.dataRequest('edge', 'store', { edgeId: data.edgeIds })
                );
            } else if (data && data.edges) {
                promises.push(Promise.resolve(data.edges));
            }

            Promise.all(promises)
                .done(function(result) {
                    var vertices = result[0] || [],
                        edge = result[1],
                        edges = edge ? [edge] : [];

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

                    self.trigger('objectsSelected', _.clone(selectedObjects));
                })
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

            this.setPublicApi('selectedObjects', defaultNoObjectsOrData(data));

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
                    this.dataRequest('vertex', 'store', { vertexIds: vertexId })
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
                this.dataRequest('vertex', 'store', { vertexIds: data.vertexId })
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
