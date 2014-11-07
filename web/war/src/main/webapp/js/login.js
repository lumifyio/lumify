
define([
    'flight/lib/component',
    'tpl!login',
    'configuration/plugins/authentication/authentication',
    'util/withDataRequest'
], function(
    defineComponent,
    template,
    AuthenticationPlugin,
    withDataRequest
) {
    'use strict';

    return defineComponent(Login, withDataRequest);

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

            if ((/^#?[a-z]+=/i).test(location.hash)) {
                window.location.reload();
            } else {
                this.dataRequest('user', 'me')
                    .then(function() {
                        require(['app'], function(App) {

                            self.select('authenticationSelector')
                                .find('button.loading').removeClass('loading');

                            App.attachTo('#app', {
                                animateFromLogin: true,
                                addVertexIds: self.attr.toOpen &&
                                    self.attr.toOpen.type === 'ADD' ?
                                    self.attr.toOpen : null,
                                openAdminTool: self.attr.toOpen &&
                                    self.attr.toOpen.type === 'ADMIN' ?
                                    _.pick(self.attr.toOpen, 'section', 'name') : null
                            });

                            self.$node.find('.logo').one(TRANSITION_END, function() {
                                self.teardown();
                            });
                        });
                    });
            }
        };

    }

})
