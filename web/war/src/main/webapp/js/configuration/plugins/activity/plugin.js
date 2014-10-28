define([], function() {
    'use strict';

    var activityHandlers = [],
        byType = {},
        api = {
            activityHandlers: activityHandlers,

            activityHandlersByType: byType,

            registerActivityHandler: function reg(handler) {
                activityHandlers.push(handler);
                byType[handler.type] = handler;
            },

            registerActivityHandlers: function(handlers) {
                handlers.forEach(function(h) {
                    api.registerActivityHandler(h);
                })
            }
        };

    return api;
});
