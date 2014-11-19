define(['util/websocket'], function(websocketUtils) {
    'use strict';

    return withWebsocket;

    function withWebsocket() {

        var overlayPromise = new Promise(function(fulfill, reject) {
            this.after('initialize', function() {
                _.defer(function() {
                    Promise.require('util/offlineOverlay').done(fulfill);
                })
            })
        }.bind(this));

        this.after('initialize', function() {
            var self = this;
            this.on('applicationReady', function() {
                self.setPublicApi('socketSourceGuid', websocketUtils.generateSourceGuid());
                self.worker.postMessage({
                    type: 'atmosphereConfiguration',
                    configuration: this.getAtmosphereConfiguration()
                })
            });
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

        this.websocketStateOnError = function(error) {
            overlayPromise.done(function(Overlay) {
                // Might be closing because of browser refresh, delay
                // so it only happens if server went down
                _.delay(function() {
                    Overlay.attachTo(document);
                }, 1000);
            });
        };

        this.websocketStateOnClose = function(message) {
            if (message && message.error) {
                console.error('Websocket closed', message.reasonPhrase, message.error);
            }
        };
    }
});
