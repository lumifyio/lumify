
define([
    '../util/ajax',
    '../util/store'
], function(ajax, store) {
    'use strict';

    var api = {

        audit: function(vertexId) {
            return ajax('GET', '/vertex/audit', {
                graphVertexId: vertexId
            });
        },

        search: function(options) {
            var params = {},
                q = _.isUndefined(options.query.query) ?
                    options.query :
                    options.query.query;

            if (options.conceptFilter) params.conceptType = options.conceptFilter;
            if (options.paging) {
                if (options.paging.offset) params.offset = options.paging.offset;
                if (options.paging.size) params.size = options.paging.size;
            }

            if (q) {
                params.q = q;
            }
            if (options.query && options.query.relatedToVertexId) {
                params.relatedToVertexId = options.query.relatedToVertexId;
            }
            params.filter = JSON.stringify(options.propertyFilters || []);

            return ajax('GET', '/vertex/search', params);
        },

        multiple: function(options) {
            return ajax('POST', '/vertex/multiple', options);
        },

        edges: function(vertexId, options) {
            var parameters = {
                graphVertexId: vertexId
            };
            if (options) {
                if (options.offset) parameters.offset = options.offset;
                if (options.size) parameters.size = options.size;
                if (options.edgeLabel) parameters.edgeLabel = options.edgeLabel;
            }

            return ajax('GET', '/vertex/edges', parameters);
        },

        deleteProperty: function(vertexId, property) {
            return ajax('DELETE', '/vertex/property', {
                graphVertexId: vertexId,
                propertyName: property.name,
                propertyKey: property.key
            })
        },

        'highlighted-text': function(vertexId, propertyKey) {
            return ajax('GET->HTML', '/vertex/highlighted-text', {
                graphVertexId: vertexId,
                propertyKey: propertyKey
            });
        },

        store: function(opts) {
            var options = _.extend({
                    workspaceId: publicData.currentWorkspaceId
                }, opts),
                returnSingular = false,
                vertexIds = options.vertexIds;

            if (!_.isArray(vertexIds)) {
                if (vertexIds) {
                    returnSingular = true;
                    vertexIds = [vertexIds];
                } else {
                    throw new Error('vertexIds must contain an object');
                }
            }

            if (!returnSingular && vertexIds.length === 0) {
                return Promise.resolve([]);
            }

            var vertices = store.getObjects(options.workspaceId, 'vertex', vertexIds),
                toRequest = [];

            vertices.forEach(function(vertex, i) {
                if (!vertex) {
                    toRequest.push(vertexIds[i]);
                }
            });

            if (toRequest.length === 0) {
                return Promise.resolve(returnSingular ? vertices[0] : vertices);
            } else {
                return api.multiple({
                    vertexIds: toRequest
                }).then(function(requested) {
                    results = vertices.map(function(vertex) {
                        if (vertex) {
                            return vertex;
                        }

                        return requested.vertices.shift();
                    });
                    return returnSingular ? results[0] : results;
                })
            }
        }
    };

    return api;
});
