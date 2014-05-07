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
            this.enableButton(false);

            this.on('click', {
                loginButtonSelector: this.onLoginButton
            });

            this.on('keyup change paste', {
                usernameSelector: this.onUsernameChange
            });

            this.select('usernameSelector').focus();
        });

        this.onUsernameChange = function(event) {
            var self = this,
                input = this.select('usernameSelector');

            _.defer(function() {
                self.enableButton($.trim(input.val()).length > 0);
            })
        };

        this.onLoginButton = function(event) {
            var self = this,
                $error = this.select('errorSelector'),
                $username = this.select('usernameSelector');

            event.preventDefault();
            event.stopPropagation();
            event.target.blur();

            this.enableButton(false, true);
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
