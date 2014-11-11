/**
 * Make data requests using events, but wrapped in promise interface
 * Depends on document level component to respond, or times out
 */
define(['util/promise'], function() {
    'use strict';

    var NO_DATA_RESPONSE_TIMEOUT_SECONDS = 4,
        currentDataRequestId = 0,
        requests = {};

    $(function() {
        $(document)
            .on('dataRequestStarted', function(event, data) {
                var request = requests[data.requestId];
                if (!request) return;
                clearTimeout(request.timeoutTimer);
            })
            .on('dataRequestCompleted', function(event, data) {
                var request = requests[data.requestId];
                if (!request) return;

                clearTimeout(request.timeoutTimer);
                if (data.success) {
                    request.promiseFulfill(data.result);
                } else {
                    request.promiseReject(data.error);
                }
            });
    });

    function dataRequestFromNode(node, service, method /*, args */) {
        if (!service || !method) {
            throw new Error('Service and method parameters required for dataRequest');
        }

        var $node = $(node),
            argsStartIndex = 3,
            args = arguments.length > argsStartIndex ? _.rest(arguments, argsStartIndex) : [],
            thisRequestId = currentDataRequestId++,
            promise = new Promise(function(fulfill, reject) {
                requests[thisRequestId] = {
                    promiseFulfill: fulfill,
                    promiseReject: reject,
                    timeoutTimer: _.delay(function() {
                        console.error('Data request went unhandled', service + '->' + method);
                        $node.trigger('dataRequestCompleted', {
                            requestId: thisRequestId,
                            success: false,
                            error: 'No data request handler responded'
                        })
                    }, NO_DATA_RESPONSE_TIMEOUT_SECONDS * 1000)
                };
            });

        wrapPromise();
        triggerEvent();

        return promise;

        function wrapPromise() {
            promise.cancel = function() {
                cleanRequest();
                $node.trigger('dataRequestCancel', {
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
            $node.trigger('dataRequest', {
                requestId: thisRequestId,
                service: service,
                method: method,
                parameters: args
            });
        }

    }

    withDataRequest.dataRequest = _.partial(dataRequestFromNode, document);

    return withDataRequest;

    function withDataRequest() {
        this.dataRequest = function(service, method /*, args */) {
            Array.prototype.splice.call(arguments, 0, 0, this.$node);
            return dataRequestFromNode.apply(this, arguments);
        }

        this.requestStoreVertices = function(ids) {
        }
    }
});
