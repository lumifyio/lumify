
define([
    '../util/ajax',
], function(ajax) {
    'use strict';

    var api = {

        geocode: function(query) {
            return ajax('GET', '/map/geocode', {
                q: query
            });
        }

    };

    return api;
});
