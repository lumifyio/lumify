
define([
    '../util/ajax',
    '../util/memoize'
], function(ajax, memoize) {
    'use strict';

    var api = {

        properties: memoize(function() {
            return ajax('GET', '/configuration')
        }),

        messages: memoize(function() {
            return api.properties()
                .then(function(p) {
                    return p.messages;
                })
        })
    };

    return api;
});
