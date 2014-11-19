
define([
    '../util/ajax',
    './storeHelper'
], function(ajax, storeHelper) {
    'use strict';

    var api = {

        audit: function(vertexId) {
            return ajax('GET', '/edge/audit', {
                graphVertexId: vertexId
            });
        },

        create: function(options) {
            return ajax('POST', '/edge/create', options);
        },

        'delete': function(edgeId, sourceId, targetId) {
            return ajax('DELETE', '/vertex/edge', {
                edgeId: edgeId,
                sourceId: sourceId,
                targetId: targetId
            });
        },

        properties: function(edgeId) {
            return ajax('GET', '/edge/properties', {
                graphEdgeId: edgeId
            });
        },

        store: storeHelper.createStoreAccessorOrDownloader(
            'edge', 'edgeId', null,
            function(toRequest) {
                if (toRequest.length > 1) {
                    throw new Error('Can only get one edge at a time');
                }
                return api.properties(toRequest[0]);
            }),

        setVisibility: function(edgeId, visibilitySource) {
            // TODO
        }

    };

    return api;
});
