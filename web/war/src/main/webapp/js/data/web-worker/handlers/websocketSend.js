define([], function() {
    'use strict';
    return function(data) {
        pushSocketMessage(data.message);
    }
});
