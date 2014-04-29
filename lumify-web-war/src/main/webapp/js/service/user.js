define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    $(function() {
        $(document).ajaxError(function(event, jqxhr, settings, exception) {
            if (jqxhr.status === 403 && !_.contains(['user/me', 'login'], settings.url)) {
                new UserService().isLoginRequired()
                    .fail(function(xhr, status, message) {
                        debugger;
                        $(document).trigger('logout', {
                            message: 'Session expired'
                        });
                    })
            }
        });
    })

    function UserService() {
        ServiceBase.call(this);

        var toMemoize = [
            'userInfo',
        ];

        this.memoizeFunctions(toMemoize);

        return this;
    }

    UserService.prototype = Object.create(ServiceBase.prototype);

    UserService.prototype.isLoginRequired = function() {
        return this._ajaxGet({ url: 'user/me' })
            .then(

                // Request was successfull, but check if user has READ
                function(user, obj, message) {
                    var deferred = $.Deferred();
                    $.extend(user, {
                        privilegesHelper: _.indexBy(user.privileges || [])
                    });
                    if (user.privilegesHelper.READ) {
                        window.currentUser = user;
                        deferred.resolve(user);
                    } else {
                        deferred.reject('Read access is required', {
                            username: user.userName,
                            focus: 'username'
                        });
                    }

                    return deferred;
                },

                // Failed to make user/me request
                function(xhr, status, message) {
                    var d = $.Deferred();
                    if (xhr.status === 403) {
                        d.reject();
                    } else {
                        d.reject();
                    }
                    return d.promise();
                }

            ).fail(function() {
                window.currentUser = null;
            });
    };

    UserService.prototype.login = function(username, password) {
        return this._ajaxPost({
            url: 'login',
            data: {
                username: username,
                password: password
            }
        }).then(
            this.isLoginRequired.bind(this),

            function(error) {
                var errorDeferred = $.Deferred();
                switch (error.status) {
                    case 403:
                        errorDeferred.reject('Invalid Username or Password', {
                            focus: 'password'
                        });
                        break;
                    case 404:
                        errorDeferred.reject('Server is unavailable');
                        break;
                    default:
                        errorDeferred.reject(error.statusText || 'Unknown Server Error');
                        break;
                }
                return errorDeferred.promise();
            }
        );
    };

    UserService.prototype.logout = function() {
        try {
            this.clearLocalStorage();
            this.disconnect();
        } catch(e) {
            console.log('Unable to disconnect socket', e);
        }
        return this._ajaxPost({ url: 'logout' });
    };

    UserService.prototype.getOnline = function() {
        var self = this,
            result = {};

        return $.when(
            this.isLoginRequired(),
            this.getCurrentUsers()
        ).then(function(user, usersResponse) {
            var users = usersResponse[0].users;

            return {
                user: user,
                users: users
            };
        });
    };

    UserService.prototype.getCurrentUsers = function() {
        return this._ajaxGet({
            url: 'users'
        });
    };

    UserService.prototype.userInfo = function(userId) {
        return this._ajaxGet({
            url: 'user/info',
            data: {
                userId: userId
            }
        });
    };

    UserService.prototype.clearLocalStorage = function() {
        if (window.localStorage) {
            _.keys(localStorage).forEach(function(key) {
                if (/^SESSION_/.test(key)) {
                    localStorage.removeItem(key);
                }
            });
        }
    };

    return UserService;
});
