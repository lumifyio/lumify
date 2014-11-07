
define(['../util/ajax'], function(ajax) {
    'use strict';

    return {
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

        cached: function(vertexId) {

        }
    };
});
