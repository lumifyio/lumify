
define([
    'flight/lib/component',
    'tpl!login',
    'configuration/plugins/authentication/authentication'
], function(
    defineComponent,
    template,
    AuthenticationPlugin
) {
    'use strict';

    return defineComponent(Login);

    function Login() {

        this.defaultAttrs({
            authenticationSelector: '.authentication'
        });

        this.before('teardown', function() {
            this.$node.remove();
        });

        this.after('initialize', function() {
            this.$node.html(template({}));

            AuthenticationPlugin.attachTo(this.select('authenticationSelector'));

            this.on('loginSuccess', this.onLoginSuccess);
        });

        this.onLoginSuccess = function() {
            var self = this;

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
        };

    }

})
