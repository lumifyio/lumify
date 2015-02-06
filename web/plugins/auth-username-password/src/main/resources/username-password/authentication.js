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
            usernameSelector: '.login input.username',
            resetUsernameSelector: '.forgot input.username',
            passwordSelector: 'input.password',
            loginButtonSelector: '.login .btn-primary',
            resetButtonSelector: '.forgot .btn-primary',
            forgotButtonSelector: '.forgotPassword',
            signInButtonSelector: '.signin',
            loginFormSelector: '.login',
            forgotFormSelector: '.forgot'
        });

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({}));
            this.enableButton(false);

            this.on('click', {
                loginButtonSelector: this.onLoginButton,
                resetButtonSelector: this.onResetButton,
                forgotButtonSelector: this.onForgotButton,
                signInButtonSelector: this.onSignInButton
            });

            this.on('keyup change paste', {
                usernameSelector: this.onUsernameChange,
                resetUsernameSelector: this.onResetUsernameChange,
                passwordSelector: this.onPasswordChange
            });

            this.select('usernameSelector').focus();

            require(['util/withDataRequest'], function(withDataRequest) {
                withDataRequest.dataRequest('config', 'properties')
                    .done(function(properties) {
                        if (properties['forgotPassword.enabled'] === 'true') {
                            self.$node.find('.forgotPassword').show();
                        }
                    });
            });
        });

        this.onSignInButton = function(event) {
            event.preventDefault();
            this.select('forgotFormSelector').hide();

            var form = this.select('loginFormSelector').show();
            _.defer(function() {
                form.find('input').eq(0).focus();
            });
        };

        this.onForgotButton = function(event) {
            var self = this;

            event.preventDefault();

            this.select('loginFormSelector').hide();

            var form = this.select('forgotFormSelector').show(),
                username = this.$node.find('.login .username').val() || '';
            _.defer(function() {
                form.find('input').eq(0).val(username).focus();
                self.checkValid();
            });
        };

        this.checkValid = function() {
            var self = this,
                user = this.select('usernameSelector'),
                resetUser = this.select('resetUsernameSelector'),
                pass = this.select('passwordSelector');

            _.defer(function() {
                self.enableButton(
                    $.trim(user.val()).length > 0 &&
                    $.trim(pass.val()).length > 0
                );
                self.enableResetButton($.trim(resetUser.val()).length > 0);
            });
        };

        this.onUsernameChange = function(event) {
            this.checkValid();
        };

        this.onResetUsernameChange = function(event) {
            this.checkValid();
        };

        this.onPasswordChange = function(event) {
            this.checkValid();
        };

        this.onResetButton = function(event) {
            event.preventDefault();

            var self = this,
                user = this.select('resetUsernameSelector');

            this.enableResetButton(false, true);
            this.$node.find('.text-error,.text-info').empty();
            Promise.resolve($.post('forgotPassword/requestToken', {
                username: user.val()
            }))
                .finally(function() {
                    self.enableResetButton(true, false);
                    self.checkValid();
                })
                .then(function() {
                    user.val('');
                    self.$node.find('.text-info').text('Sent password reset token to email');
                    self.checkValid();
                })
                .catch(function(e) {
                    var statusText = e.statusText,
                        error;

                    if (/^\s*{/.test(statusText)) {
                        try {
                            var json = JSON.parse(statusText);
                            error = json.username;
                        } catch(e) { }
                    }

                    self.$node.find('.text-error').text(
                        error || 'Unknown Server Error'
                    );
                });
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
            this.submitting = true;
            $error.empty();

            $.post('login', {
                username: $username.val(),
                password: $password.val()
            }).always(function() {
                self.submitting = false;
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
            if (this.submitting) return;
            var button = this.select('loginButtonSelector');

            if (enable) {
                button.removeClass('loading').removeAttr('disabled');
            } else {
                button.toggleClass('loading', !!loading)
                    .attr('disabled', true);
            }
        }

        this.enableResetButton = function(enable, loading) {
            var button = this.select('resetButtonSelector');

            if (enable) {
                button.removeClass('loading').removeAttr('disabled');
            } else {
                button.toggleClass('loading', !!loading)
                    .attr('disabled', true);
            }
        }
    }

})
