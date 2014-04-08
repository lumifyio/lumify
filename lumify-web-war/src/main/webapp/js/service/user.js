define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    $(function() {
        $(document).ajaxError(function(event, jqxhr, settings, exception) {
            if (jqxhr.status === 403 && !_.contains(['user/me', 'login'], settings.url)) {
                new UserService().isLoginRequired()
                    .fail(function() {
                        $(document).trigger('logout');
                    })
            }
        });
    })

    function UserService() {
        ServiceBase.call(this);

        return this;
    }

    UserService.prototype = Object.create(ServiceBase.prototype);

    UserService.prototype.isLoginRequired = function() {
        return this._ajaxGet({ url: 'user/me' });
    };

    UserService.prototype.login = function(username, password) {
        return this._ajaxPost({ 
            url: 'login',
            data: {
                username: username,
                password: password
            }
        });
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
            this._ajaxGet({ url: 'user/me' }),
            this.getCurrentUsers()
        ).then(function(userResponse, usersResponse) {
            var user = userResponse[0],
                users = usersResponse[0].users;

            return {
                user: user,
                users: users
            };
        });
    };

    UserService.prototype.getCurrentUsers = function() {
        return this._ajaxGet({
            url: 'user'
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
