/**
 * Make data requests using events, but wrapped in promise interface
 * Depends on document level component to respond, or times out
 */
define(['util/promise', 'underscore'], function(Promise, _) {
    'use strict';

    var NO_DATA_RESPONSE_TIMEOUT_SECONDS = 4,
        currentDataRequestId = 0,
        requests = {};

    $(function() {
        $(document)
            .on('dataRequestStarted', function(event, data) {
                console.log('dataRequestStarted', data)
                var request = requests[data.requestId];
                if (request) {
                    clearTimeout(request.timeoutTimer);
                }
            })
            .on('dataRequestCompleted', function(event, data) {
                var request = cleanRequest(data.requestId);
                if (request) {
                    if (data.success) {
                        request.promiseFulfill(data.result);
                    } else {
                        request.promiseReject(data.error);
                    }
                }
            });
    });

    function cleanRequest(requestId) {
        var request = requests[requestId];
        if (request) {
            clearTimeout(request.timeoutTimer);
            delete requests[requestId];
            return request;
        }
    }

    function dataRequestFromNode(node, service, method /*, args */) {
        if (!service || !method) {
            throw new Error('Service and method parameters required for dataRequest');
        }

        var argsStartIndex = 3,
            thisRequestId = currentDataRequestId++,
            args = arguments.length > argsStartIndex ? _.rest(arguments, argsStartIndex) : [];

        return Promise.require('util/requirejs/promise!util/service/dataPromise')
            .then(function() {
                console.log('requesting', service, method, args, thisRequestId)
                var $node = $(node),
                    nodeEl = $node[0],
                    $nodeInDom = $.contains(document.documentElement, nodeEl) ? $node : $(document),
                    promise = new Promise(function(fulfill, reject) {
                        requests[thisRequestId] = {
                            promiseFulfill: fulfill,
                            promiseReject: reject,
                            timeoutTimer: _.delay(function() {
                                console.error('Data request went unhandled', service + '->' + method);
                                $nodeInDom.trigger('dataRequestCompleted', {
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
                        cleanRequest(thisRequestId);
                        $nodeInDom.trigger('dataRequestCancel', {
                            requestId: thisRequestId
                        });
                    };
                }

                function triggerEvent() {
                    $nodeInDom.trigger('dataRequest', {
                        requestId: thisRequestId,
                        service: service,
                        method: method,
                        parameters: args
                    });
                }
        })
    }

    withDataRequest.dataRequest = _.partial(dataRequestFromNode, document);

    return withDataRequest;

    function withDataRequest() {

        if (!('dataRequest' in this)) {
            this.dataRequest = function(service, method /*, args */) {
                Array.prototype.splice.call(arguments, 0, 0, this.$node);
                return dataRequestFromNode.apply(this, arguments);
            }
        }
    }
});
