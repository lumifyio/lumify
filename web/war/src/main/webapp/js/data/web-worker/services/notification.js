
define([
    '../util/ajax'
], function(ajax) {
    'use strict';

    var api = {
        list: function() {
            return ajax('GET', '/notification/all');
        },

        markRead: function(ids) {
            return ajax('POST', '/notification/mark-read', {
                notificationIds: _.isArray(ids) ? ids : [ids]
            })
        }
    };

    return api;
});
