
define([], function() {
    'use strict';

    var keys = {};

    function throttle(key, throttleMillis, func) {
        return function(event) {
            var self = this;

            clearTimeout(keys[key]);
            keys[key] = setTimeout(function() {
                func.call(self, event);
            }, throttleMillis);
        };
    }

    return throttle;
});
