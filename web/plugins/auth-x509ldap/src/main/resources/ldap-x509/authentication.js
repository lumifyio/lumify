define([
    'configuration/plugin',
    'hbs!./templates/login',
    'less!./less/login'
], function(
    defineLumifyPlugin,
    template,
    less) {
    'use strict';

    return defineLumifyPlugin(LdapX509Authentication, {
        less: less
    });

    function LdapX509Authentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
            loginButtonSelector: 'button'
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
            this.on('click', {
                loginButtonSelector: this.onLoginButton
            });

            this.login();
        });

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

            this.configureButton({
                text: 'Verifying Certificate...',
                enabled: false,
                loading: true
            });

            $error.empty();

            $.post('login')
                .fail(function(xhr, status, error) {
                    $error.text(error);
                    self.configureButton({
                        text: 'Try Again',
                        enabled: true
                    });
                })
                .done(function() {
                    self.trigger('loginSuccess');
                })
        };

        this.configureButton = function(options) {
            var button = this.select('loginButtonSelector')
                             .text(options.text);

            if (options.enabled) {
                button.removeClass('loading').removeAttr('disabled');
            } else {
                button
                    .toggleClass('loading', !!options.loading)
                    .attr('disabled', true);
            }
        };

    }

})
