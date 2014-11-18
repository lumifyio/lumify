define([], function() {
    'use strict';
    return function(message) {
        if (typeof atmosphere !== 'undefined') {
            // TODO: add sourceId to filter current users requests
            publicData.socket = atmosphere.subscribe(_.extend(message.configuration, {
                onOpen: function(response) {
                    if (atmosphere.util.__socketPromiseFulfill) {
                        atmosphere.util.__socketPromiseFulfill(publicData.socket);
                    } {
                        atmosphere.util.__socketOpened = true;
                    }
                },
                onError: function() {
                    // TODO: show overlay
                },
                onClose: function() {
                    atmosphere.util.__socketOpened = false;
                },
                onMessage: function(response) {
                    processMainMessage({
                        type: 'websocketMessage',
                        responseBody: response.responseBody
                    });
                }
            }));
        }
    };
})
