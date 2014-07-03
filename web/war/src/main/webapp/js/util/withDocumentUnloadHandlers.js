define([], function() {
    'use strict';

    return withDocumentUnloadHandlers;

    /**
     * Mixin that provides a window unload listener and manages
     * a priority sorted queue of callbacks to be applied.
     * If any of the callbacks return a String, the remaining
     * callbacks will not be executed and the user will be
     * propmted with that String.
     */
    function withDocumentUnloadHandlers() {
        var registeredHandlers = [],
            uniqueId = _.uniqueId();

        this.before('initialize', function() {
            $(document).on('registerBeforeUnloadHandler', this.onRegisterBeforeUnloadHandler);
            $(document).on('unregisterBeforeUnloadHandler', this.onUnregisterBeforeUnloadHandler);
            $(window).on('beforeunload.' + uniqueId, this.onDocumentUnload);
        });

        this.getUnloadHandlers = function() {
            return registeredHandlers;
        };

        this.clearUnloadHandlers = function() {
            registeredHandlers = [];
        };

        this.onDocumentUnload = function(evt) {
            var self = this,
                confirmationMessage;

            $.each(registeredHandlers, function(index, handler) {
                confirmationMessage = handler.fn.call(handler.scope || self, evt);
                return !_.isString(confirmationMessage);
            });

            if (_.isString(confirmationMessage)) {
                (evt || window.event).returnValue = confirmationMessage;   //Gecko + IE
                return confirmationMessage;                                //Webkit, Safari, Chrome etc.
            }
        };

        this.onRegisterBeforeUnloadHandler = function(event, data) {
            if ($.isFunction(data)) {
                data = { fn: data };
            }

            if (data) {
                data.priority = _.isNumber(data.priority) ?  data.priority : Number.MAX_VALUE;

                registeredHandlers.push(data);
                registeredHandlers = _.chain(registeredHandlers)
                                            .uniq(false, _.property('fn'))
                                            .sortBy('priority')
                                            .value();
            }
        };

        this.onUnregisterBeforeUnloadHandler = function(event, data) {
            if (data && !$.isFunction(data)) {
                data = data.fn;
            }

            if (data) {
                var existingHandler = _.find(registeredHandlers, function(handler) {
                    return handler.fn === data;
                });

                if (existingHandler) {
                    registeredHandlers = _.without(registeredHandlers, existingHandler);
                }
            }
        };
    }
});
