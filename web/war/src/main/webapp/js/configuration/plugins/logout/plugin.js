define(['underscore'], function(_) {
    'use strict';

    var handlers = [];

    return {
        handlers: handlers,

        registerLogoutHandler: function(handler) {
            if (!_.isFunction(handler)) {
                throw new Error('Logout handler must be a function');
            }

            handlers.push(handler);
        },

        /**
         * Do not call, automatically called by app. Return false from handler
         * to prevent default logout behavior
         */
        executeHandlers: function() {
            if (!handlers.length) {
                return true;
            }

            var anyReturnedFalse = _.any(handlers, function(handler) {
                return handler() === false;
            });

            if (anyReturnedFalse) {
                return false;
            }

            return true;
        }

    };
});
