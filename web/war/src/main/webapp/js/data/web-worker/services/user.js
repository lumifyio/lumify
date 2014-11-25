
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

        get: function(userName) {
            return ajax('GET', '/user', {
                'user-name': userName
            });
        },

        preference: function(name, value) {
            return ajax('POST', '/user/ui-preferences', {
                name: name,
                value: value
            });
        },

        search: function(options) {
            var data = {},
                returnSingular = false;

            if (options.query) {
                data.q = options.query;
            }
            if (options.userIds) {
                if (!_.isArray(options.userIds)) {
                    returnSingular = true;
                    data.userIds = [options.userIds];
                } else {
                    data.userIds = options.userIds;
                }
            }
            return ajax(
                (data.userIds && data.userIds.length > 2) ? 'POST' : 'GET',
                '/user/all', data)
                .then(function(response) {
                    var users = response.users;
                    return returnSingular ? users[0] : users;
                })
        },

        logout: function(options) {
            return ajax('POST', '/logout');
        }

    };
});
