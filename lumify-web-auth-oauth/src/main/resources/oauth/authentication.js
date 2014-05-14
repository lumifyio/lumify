define([
    'configuration/plugin',
    'hbs!./templates/login',
    'less!./less/oauth'
], function(
    defineLumifyPlugin,
    template,
    less) {
    'use strict';

    return defineLumifyPlugin(OAuthAuthentication, {
        less: less
    });

    function OAuthAuthentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
        });

    }
});
