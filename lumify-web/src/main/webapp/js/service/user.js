define(
    [
        'service/serviceBase'
    ],
    function (ServiceBase) {
        'use strict';

        function UserService() {
            ServiceBase.call(this);

            return this;
        }

        UserService.prototype = Object.create(ServiceBase.prototype);

        UserService.prototype.isLoginRequired = function() {
            return this._ajaxGet({ url:'user/me' });
        };

        UserService.prototype.login = function(username, password) {
            return this._ajaxPost({ 
                url:'login',
                data: {
                    username: username,
                    password: password
                }
            });
        };

        UserService.prototype.logout = function() {
            try {
                this.disconnect();
            } catch(e) {
                console.log('Unable to disconnect socket', e);
            }
            return this._ajaxPost({ url:'logout' });
        };

        UserService.prototype.getOnline = function() {
            var self = this;
            var result = {};

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
                url: '/user/'
            });
        };

        return UserService;
    });

