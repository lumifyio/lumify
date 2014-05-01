
define([
    'util/formatters',
], function(F) {
    'use strict';

    var V = {
            url: function(vertices, workspaceId) {
                return window.location.href.replace(/#.*$/,'') +
                    '#v=' + _.map(vertices, function(v) {
                        return encodeURIComponent(_.isString(v) ? v : v.id);
                    }).join(',') +
                    '&w=' + encodeURIComponent(workspaceId);
            },

            fragmentUrl: function(vertices, workspaceId) {
                return V.url(vertices, workspaceId).replace(/^.*#/, '#');
            },

            parametersInUrl: function(url) {
                var match = url.match(/#v=(.+?)(?:&w=(.*))?$/);
                if (match && match.length === 3) {
                    return {
                        vertexIds: _.map(match[1].split(','), function(v) {
                            return decodeURIComponent(v);
                        }),
                        workspaceId: decodeURIComponent(match[2] || '')
                    }
                }
                return null;
            }
    };

    return $.extend({}, F, { vertexUrl: V });
});
