
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
        });

    }

})
