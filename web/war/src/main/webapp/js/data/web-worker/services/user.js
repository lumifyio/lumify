
define(['../util/ajax'], function(ajax) {
    'use strict';

    var api = {
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

        getUserNames: (function() {
            var cachedNames = {};
            return function getUserNames(userIds) {
                var notCached = _.reject(userIds, function(userId) {
                    return userId in cachedNames || (
                        publicData.currentUser.id === userId
                    );
                });

                if (notCached.length) {
                    return api.search({ userIds: notCached })
                        .then(function(users) {
                            var usersById = _.indexBy(users, 'id');
                            return userIds.map(function(userId) {
                                return cachedNames[userId] || (
                                    cachedNames[userId] = (usersById[userId] || publicData.currentUser).displayName
                                );
                            });
                        });
                } else {
                    return Promise.resolve(
                        userIds.map(function(userId) {
                            return cachedNames[userId] || publicData.currentUser.displayName;
                        })
                    );
                }
            };
        })(),

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
                    data.userIds = _.unique(options.userIds);
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

    return api;
});
