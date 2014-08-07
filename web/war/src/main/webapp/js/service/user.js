define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    $(function() {
        $(document).ajaxError(function(event, jqxhr, settings, exception) {
            if (jqxhr.status === 403 && !_.contains(['user/me', 'login'], settings.url)) {
                new UserService().isLoginRequired()
                    .fail(function(xhr, status, message) {
                        $(document).trigger('logout', {
                            message: i18n('lumify.session.expired')
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

        this.serviceName = 'user';
        this.memoizeFunctions(this.serviceName, toMemoize);

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
                        deferred.reject(i18n('lumify.access.read_required'), {
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
                        errorDeferred.reject(i18n('lumify.credentials.invalid'), {
                            focus: 'password'
                        });
                        break;
                    case 404:
                        errorDeferred.reject(i18n('lumify.server.not_found'));
                        break;
                    default:
                        errorDeferred.reject(error.statusText || i18n('lumify.server.error'));
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
        return this.isLoginRequired();
    };

    UserService.prototype.search = function(query, workspaceId) {
        var data = {};
        if (query) {
            data.query = query;
        }
        if (workspaceId) {
            data.workspaceId = workspaceId;
        }
        return this._ajaxGet({
            url: 'users',
            data: data
        });
    };

    UserService.prototype.getCurrentUsers = function(workspaceId) {
        return this._ajaxGet({
            url: 'users',
            data: {
                workspaceId: workspaceId
            }
        });
    };

    UserService.prototype.getUser = function(userName) {
        return this._ajaxGet({
            url: 'user',
            data: {
                'user-name': userName
            }
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
