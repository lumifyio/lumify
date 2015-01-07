define(['jquery'], function($) {
    'use strict';

    var activityHandlers = [],
        byType = {},
        byKind = {},
        triggerUpdate = _.throttle(function() {
            $(document).trigger('activityHandlersUpdated');
        }, 2000),
        api = {
            activityHandlers: activityHandlers,

            activityHandlersByType: byType,

            activityHandlersByKind: byKind,

            registerActivityHandler: function reg(handler) {
                activityHandlers.push(handler);
                byType[handler.type] = handler;
                if (!(handler.kind in byKind)) {
                    byKind[handler.kind] = [handler];
                } else {
                    byKind[handler.kind].push(handler);
                }
                triggerUpdate();
            },

            registerActivityHandlers: function(handlers) {
                handlers.forEach(function(h) {
                    api.registerActivityHandler(h);
                })
            }
        };

    return api;
});
