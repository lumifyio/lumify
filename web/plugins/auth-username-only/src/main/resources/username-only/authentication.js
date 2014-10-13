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

            this.on('keydown keyup change paste', {
                usernameSelector: this.onUsernameChange
            });

            this.select('usernameSelector').focus();

            var match = window.location.hash.match(/^#username=(.*)/),
                username = match != null ? match[1] : null;
            if (username != null) {
                this.select('usernameSelector').val(username);
                this.select('loginButtonSelector').click();
                // TODO: remove fragment after login
            }
        });

        this.onUsernameChange = function(event) {
            var self = this,
                input = this.select('usernameSelector'),
                isValid = function() {
                    return $.trim(input.val()).length > 0;
                };

            if (event.which === $.ui.keyCode.ENTER) {
                event.preventDefault();
                event.stopPropagation();
                if (isValid()) {
                    return _.defer(this.login.bind(this));
                }
            }

            _.defer(function() {
                self.enableButton(isValid());
            });
        };

        this.onLoginButton = function(event) {
            event.preventDefault();
            event.stopPropagation();
            event.target.blur();

            this.login();
        };

        this.login = function() {
            var self = this,
                $error = this.select('errorSelector'),
                $username = this.select('usernameSelector');

            if (this.disabled) {
                return;
            }

            this.disabled = true;
            this.enableButton(false, true);
            $error.empty();

            $.post('login', { username: $username.val() })
                .fail(function(xhr, status, error) {
                    $error.text(error);
                    self.enableButton(true);
                    self.disabled = false;
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
