define([
    'configuration/plugin',
    'hbs!./templates/login',
    'less!./less/login'
], function(
    defineLumifyPlugin,
    template,
    less) {
    'use strict';

    return defineLumifyPlugin(UserNameAndPasswordAuthentication, {
        less: less
    });

    function UserNameAndPasswordAuthentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
            usernameSelector: 'input.username',
            passwordSelector: 'input.password',
            loginButtonSelector: 'button'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.enableButton(false);

            this.on('click', {
                loginButtonSelector: this.onLoginButton
            });

            this.on('keyup change paste', {
                usernameSelector: this.onUsernameChange,
                passwordSelector: this.onPasswordChange
            });

            this.select('usernameSelector').focus();
        });

        this.checkValid = function() {
            var self = this,
                user = this.select('usernameSelector'),
                pass = this.select('passwordSelector');

            _.defer(function() {
                self.enableButton(
                    $.trim(user.val()).length > 0 &&
                    $.trim(pass.val()).length > 0
                );
            });
        };

        this.onUsernameChange = function(event) {
            this.checkValid();
        };

        this.onPasswordChange = function(event) {
            this.checkValid();
        };

        this.onLoginButton = function(event) {
            var self = this,
                $error = this.select('errorSelector'),
                $username = this.select('usernameSelector'),
                $password = this.select('passwordSelector');

            event.preventDefault();
            event.stopPropagation();
            event.target.blur();

            this.enableButton(false, true);
            $error.empty();

            $.post('login', {
                username: $username.val(),
                password: $password.val()
            }).fail(function(xhr, status, error) {
                if (xhr.status === 403) {
                    error = 'Invalid Username / Password';
                }
                $error.text(error);
                self.enableButton(true);
            })
            .done(function() {
                self.trigger('loginSuccess');
            })
        };

        this.enableButton = function(enable, loading) {
            var button = this.select('loginButtonSelector');

            if (enable) {
                button.removeClass('loading').removeAttr('disabled');
            } else {
                button.toggleClass('loading', !!loading)
                    .attr('disabled', true);
            }
        }
    }

})
