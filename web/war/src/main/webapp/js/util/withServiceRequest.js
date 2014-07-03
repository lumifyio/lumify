/**
 * Make service requests using events, but wrapped in promise interface
 * Depends on document level component to respond, or times out
 */
define([], function() {
    'use strict';

    var NO_SERVICE_RESPONSE_TIMEOUT_SECONDS = 1;

    return withServiceRequest;

    function withServiceRequest() {

        var serviceRequestId = 0,
            requests = {};

        this.after('initialize', function() {
            this.on(document, 'serviceRequestStarted', function(event, data) {
                var request = requests[data.requestId];
                if (!request) return;
                clearTimeout(request.timeoutTimer);
            })
            this.on(document, 'serviceRequestCompleted', function(event, data) {
                var request = requests[data.requestId];
                if (!request) return;

                clearTimeout(request.timeoutTimer);
                if (data.success) {
                    request.deferred.resolve(data.result);
                } else {
                    request.deferred.reject(data.error);
                }
            })
        });

        this.serviceRequest = function(service, method) /*, args */ {
            var self = this,
                args = arguments.length > 2 ? _.rest(arguments, 2) : [],
                thisRequestId = serviceRequestId++,
                d = $.Deferred(),
                promise = d.promise();

            saveRequest();
            wrapPromise();
            triggerEvent();

            return promise;

            function saveRequest() {
                requests[thisRequestId] = {
                    deferred: d,
                    timeoutTimer: _.delay(function() {
                        console.error('Service request went unhandled', service + '->' + method);
                        self.trigger('serviceRequestCompleted', {
                            requestId: thisRequestId,
                            success: false,
                            error: 'No service request handler responded'
                        })
                    }, NO_SERVICE_RESPONSE_TIMEOUT_SECONDS * 1000)
                };
            }

            function wrapPromise() {
                promise.cancel = function() {
                    cleanRequest();
                    self.trigger('serviceRequestCancel', {
                        requestId: thisRequestId
                    });
                };

                promise.always(cleanRequest)
            }

            function cleanRequest() {
                var request = requests[thisRequestId];
                if (request) {
                    clearTimeout(request.timeoutTimer);
                    delete requests[thisRequestId];
                }
            }

            function triggerEvent() {
                self.trigger('serviceRequest', {
                    requestId: thisRequestId,
                    service: service,
                    method: method,
                    parameters: args
                });
            }
        }

    }
});
