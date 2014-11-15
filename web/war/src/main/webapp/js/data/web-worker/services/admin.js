
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {

        vertexDelete: function(vertexId, workspaceId) {
            return ajax('POST->HTML', '/admin/deleteVertex', {
                graphVertexId: vertexId,
                workspaceId: workspaceId
            });
        },

        dictionary: function() {
            return ajax('GET', '/admin/dictionary');
        },

        plugins: function() {
            return ajax('GET', '/admin/plugins');
        }

    };

    return api;
});
