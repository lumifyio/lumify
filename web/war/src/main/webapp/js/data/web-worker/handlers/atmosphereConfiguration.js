define([], function() {
    'use strict';
    return function(message) {
        if (typeof atmosphere !== 'undefined') {
            atmosphere.subscribe(message.configuration);
        }
    };
})
