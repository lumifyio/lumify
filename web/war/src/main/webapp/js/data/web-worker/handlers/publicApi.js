define([], function() {
    'use strict';

    return function(message) {
        publicData[message.key] = message.obj;
    }
});
