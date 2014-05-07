define([
    'configuration/plugin',
    'hbs!./templates/login',
    'less!./less/login'
], function(
    defineLumifyPlugin,
    template,
    less) {
    'use strict';

    return defineLumifyPlugin(UserNameOnlyAuthentication, {
        less: less
    });

    function UserNameOnlyAuthentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
            usernameSelector: 'input.username',
            loginButtonSelector: 'button'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));

            this.on('click', {
                loginButtonSelector: this.onLoginButton
            });
        });

        this.onLoginButton = function(event) {
            var self = this,
                $error = this.select('errorSelector'),
                $username = this.select('usernameSelector');

            event.preventDefault();
            event.stopPropagation();

            this.enableButton(false);
            $error.empty();

            $.post('login', { username: $username.val() })
                .fail(function(xhr, status, error) {
                    $error.text(error);
                    self.enableButton(true);
                })
                .done(function() {
                    self.trigger('loginSuccess');
                })
        };

        this.enableButton = function(enable) {
            var button = this.select('buttonSelector');

            if (enable) {
                button.addClass('loading').attr('disabled', true);
            } else {
                button.removeClass('loading').removeAttr('disabled');
            }
        }

    }
});
