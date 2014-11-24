
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

        search: function(query, optionalWorkspaceId) {
            var data = {};
            if (query) {
                data.q = query;
            }
            if (optionalWorkspaceId) {
                data.workspaceId = optionalWorkspaceId;
            }
            return ajax('GET', '/user/all', data)
                .then(function(response) {
                    return response.users;
                })
        },

        info: function(userIds) {
            var returnSingular = false;
            if (!_.isArray(userIds)) {
                returnSingular = true;
                userIds = [userIds];
            }

            return ajax(userIds.length > 1 ? 'POST' : 'GET', '/user/info', {
                userIds: userIds
            }).then(function(result) {
                if (returnSingular) {
                    return result.users[userIds[0]];
                }
                return result.users;
            });
        },

        logout: function(options) {
            return ajax('POST', '/logout');
        }

    };
});
