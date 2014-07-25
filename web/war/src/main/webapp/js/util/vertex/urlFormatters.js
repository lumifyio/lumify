
define([
    'util/formatters',
], function(F) {
    'use strict';

    var URL_TYPES = {
            FULLSCREEN: 'v',
            ADD: 'add'
        },
        V = {
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

            isFullscreenUrl: function(url) {
                var toOpen = V.parametersInUrl(url);

                return toOpen &&
                    toOpen.vertexIds &&
                    toOpen.vertexIds.length &&
                    toOpen.type === URL_TYPES.FULLSCREEN;
            },

            parametersInUrl: function(url) {
                var type = _.invert(URL_TYPES),
                    match = url.match(/#(v|add)=(.+?)(?:&w=(.*))?$/);

                if (match && match.length === 4) {
                    return {
                        vertexIds: _.map(match[2].split(','), function(v) {
                            return decodeURIComponent(v);
                        }),
                        workspaceId: decodeURIComponent(match[3] || ''),
                        type: type[match[1]]
                    };
                }
                return null;
            }
    };

    return $.extend({}, F, { vertexUrl: V });
});
