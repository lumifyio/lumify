
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

        logout: function(options) {
            return ajax('POST', '/logout');
        }

    };
});
