define([], function() {
    'use strict';

    return withLegacyWebsocket;

    function withLegacyWebsocket() {

        this.websocketNotSupportedInWorker = function() {
            var self = this,
                config = this.getAtmosphereConfiguration(),
                atmospherePromise = new Promise(function(fulfill, reject) {
                    require(['atmosphere'], function(atmosphere) {
                        var socket = atmosphere.subscribe(_.extend(config, {

                            // Remember to also Change
                            // web-worker/handlers/atmosphereConfiguration
                            onOpen: function() {
                                console.log('OPENED');
                                fulfill(socket);
                            },
                            onError: function(request) {
                                self.websocketStateOnError({
                                    reason: request.reasonPhrase,
                                    error: request.error
                                });
                            },
                            onClose: function(request) {
                                self.websocketStateOnClose({
                                    reason: request.reasonPhrase,
                                    error: request.error
                                });
                            },
                            onMessage: function(response) {
                                self.worker.postMessage({
                                    type: 'websocketMessage',
                                    responseBody: response.responseBody
                                });
                            }
                        }));
                    });
                });

            this.around('pushSocket', function(push, message) {
                atmospherePromise.then(function(socket) {
                    socket.push(message);
                })
            });

            this.websocketFromWorker = function(message) {
                Promise.all([
                    atmospherePromise,
                    'util/websocket'
                ]).then(function(r) {
                    var socket = r[0],
                        websocketUtils = r[1];

                    websocketUtils.pushDataToSocket(socket, lumifyData.socketSourceGuid, message);
                });
            }
        }

    }
});
