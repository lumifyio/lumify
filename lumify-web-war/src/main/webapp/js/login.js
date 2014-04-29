
define([
    'flight/lib/component',
    'tpl!login',
    'service/user'
], function(
    defineComponent,
    template,
    UserService
) {
    'use strict';

    return defineComponent(Login);

    function Login() {

        this.defaultAttrs({
            loginButtonSelector: 'button',
            usernameSelector: 'input.username',
            passwordSelector: 'input.password',
            errorSelector: '.text-error'
        });

        this.before('teardown', function() {
            this.$node.remove();
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.select('loginButtonSelector').attr('disabled', true);

            this.select('errorSelector')
                .text(this.attr.errorMessage)
                .toggleClass('no-error', !this.attr.errorMessage);

            if (this.attr.errorMessageOptions) {
                this.handleErrorOptions(this.attr.errorMessageOptions);
            } else {
                _.defer(function() {
                    this.select('usernameSelector').focus();
                }.bind(this));
            }

            this.on('click', {
                loginButtonSelector: this.onLogin
            });

            this.on('change keyup', {
                usernameSelector: this.checkValid,
                passwordSelector: this.checkValid
            });
        });

        this.checkValid = function(e) {
            var invalid = this.select('usernameSelector').val().length === 0 ||
                          this.select('passwordSelector').val().length === 0,
                button = this.select('loginButtonSelector')

            if (button.hasClass('loading')) return;

            button.attr('disabled', invalid);
        };

        this.onLogin = function(e) {
            e.preventDefault();

            var self = this,
                button = self.select('loginButtonSelector').attr('disabled', true).addClass('loading'),
                error = self.select('errorSelector').addClass('no-error'),
                user = self.select('usernameSelector'),
                password = self.select('passwordSelector');

            new UserService().login(user.val(), password.val())

                .done(function() {
                    if ((/^#?v=/).test(location.hash)) {
                        window.location.reload();
                    } else {
                        require(['app'], function(App) {
                            App.attachTo('#app', {
                                animateFromLogin: true
                            });

                            self.$node.find('.logo').one(TRANSITION_END, function() {
                                self.teardown();
                            });
                        });
                    }
                })

                .fail(function(errorMessage, options) {
                    button.removeClass('loading').attr('disabled', false);

                    error
                        .text(errorMessage)
                        .removeClass('no-error');

                    self.handleErrorOptions(options);
                });
        }

        this.handleErrorOptions = function(options) {
            if (options) {
                var user = this.select('usernameSelector'),
                    password = this.select('passwordSelector');

                if (options && options.username) {
                    user.val(options.username);
                }

                if (options && options.focus === 'username') {
                    user.focus().get(0).select();
                } else if (options && options.focus === 'password') {
                    password.focus().get(0).select();
                }
            }
        }
    }

})
