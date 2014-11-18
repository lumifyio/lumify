define(['util/websocket'], function(websocketUtils) {
    'use strict';

    return withWebsocket;

    function withWebsocket() {

        this.after('initialize', function() {
            var self = this;
            this.on('applicationReady', function() {
                self.setPublicApi('socketSourceGuid', websocketUtils.generateSourceGuid());
                self.worker.postMessage({
                    type: 'atmosphereConfiguration',
                    configuration: this.getAtmosphereConfiguration()
                })
            })
        });

        this.pushSocket = function(message) {
            this.worker.postMessage({
                type: 'websocketSend',
                message: message
            });
        };

        this.rebroadcastEvent = function(message) {
            this.trigger(message.eventName, message.data);
        };

        this.getAtmosphereConfiguration = function() {
            return {
                url: 'messaging',
                transport: 'websocket',
                fallbackTransport: 'long-polling',
                contentType: 'application/json',
                trackMessageLength: true,
                suspend: false,
                shared: false,
                connectTimeout: -1,
                enableProtocol: true,
                maxReconnectOnClose: 2,
                maxStreamingLength: 2000,
                logLevel: 'debug'
            };
        };

    }
});
