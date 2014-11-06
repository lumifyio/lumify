define([], function() {
    'use strict';

    return withLegacyWebsocket;

    function withLegacyWebsocket() {

        this.websocketNotSupportedInWorker = function() {
            var config = this.getAtmosphereConfiguration();
            require(['atmosphere'], function(atmosphere) {
                atmosphere.subscribe(config);
            })
        }

    }
});
