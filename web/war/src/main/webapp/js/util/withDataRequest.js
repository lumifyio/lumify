/**
 * Make data requests using events, but wrapped in promise interface
 * Depends on document level component to respond, or times out
 */
define([
    'util/promise',
    'underscore',
    'jquery',
    'util/requirejs/promise!util/service/dataPromise'
], function(Promise, _, $) {
    'use strict';

    var NO_DATA_RESPONSE_TIMEOUT_SECONDS = 4,
        currentDataRequestId = 0,
        requests = {};

    $(document)
        .on('dataRequestStarted', function(event, data) {
            var request = requests[data.requestId];
            if (request) {
                clearTimeout(request.timeoutTimer);
            }
        })
        .on('dataRequestProgress', function(event, data) {
            var request = requests[data.requestId];
            if (request) {
                request.promiseFulfill.updateProgress(data.progress);
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
            $node = $(node),
            nodeEl = $node[0],
            $nodeInDom = $.contains(document.documentElement, nodeEl) ? $node : $(document),
            args = arguments.length > argsStartIndex ? _.rest(arguments, argsStartIndex) : [],
            promise = new Promise(function(fulfill, reject) {
                Promise.require('util/requirejs/promise!util/service/dataPromise')
                    .then(function() {
                        requests[thisRequestId] = {
                            promiseFulfill: fulfill,
                            promiseReject: reject,
                            timeoutTimer: _.delay(function() {
                                console.error('Data request went unhandled', service + '->' + method);
                                $nodeInDom.trigger('dataRequestCompleted', {
                                    requestId: thisRequestId,
                                    success: false,
                                    error: 'No data request handler responded for ' + service + '->' + method
                                })
                            }, NO_DATA_RESPONSE_TIMEOUT_SECONDS * 1000)
                        };

                        $nodeInDom.trigger('dataRequest', {
                            requestId: thisRequestId,
                            service: service,
                            method: method,
                            parameters: args
                        });
                    })
            });

        promise.cancel = function() {
            var request = cleanRequest(thisRequestId);
            if (request) {
                $nodeInDom.trigger('dataRequestCancel', {
                    requestId: thisRequestId
                });
            }
        };

        return promise;
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
