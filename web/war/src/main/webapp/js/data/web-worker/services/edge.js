
define([
    '../util/ajax',
    '../util/store'
], function(ajax, store) {
    'use strict';

    var api = {

        audit: function(vertexId) {
            return ajax('GET', '/edge/audit', {
                graphVertexId: vertexId
            });
        },

        create: function(options) {
            return ajax('POST', '/edge/create', options);
        }
    };

    return api;
});
