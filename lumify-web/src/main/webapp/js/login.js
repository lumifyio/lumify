
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

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.select('loginButtonSelector').attr('disabled', true);
            this.select('errorSelector').addClass('no-error');

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
                    // TODO: make more seamless, but for now this should work
                    window.location.reload();
                })

                .fail(function() {
                    error.removeClass('no-error');
                    button.removeClass('loading').attr('disabled', false);
                    password.focus().get(0).select();
                });
        }
    }

})
