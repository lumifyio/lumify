
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {
        list: function() {
            return ajax('GET', '/notification/all');
        }
    };

    return api;
});
