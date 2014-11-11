
define(['../util/ajax'], function(ajax) {
    'use strict';

    return {
        me: function(options) {
            return ajax('GET', '/user/me')
                .then(function(user) {
                    return _.extend(user, {
                        privilegesHelper: _.indexBy(user.privileges || [])
                    });
                })
        },

        preference: function(name, value) {
            return ajax('POST', '/user/ui-preferences', {
                name: name,
                value: value
            });
        },

        info: function(userIds) {
            return ajax(userIds.length > 1 ? 'POST' : 'GET', '/user/info', {
                userIds: userIds
            });
        },

        logout: function(options) {
            return ajax('POST', '/logout');
        }

    };
});
