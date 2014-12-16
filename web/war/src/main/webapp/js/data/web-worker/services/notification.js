
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {
        systemNotificationList: function() {
            return ajax('GET', '/notification/all');
        }
    };

    return api;
});
