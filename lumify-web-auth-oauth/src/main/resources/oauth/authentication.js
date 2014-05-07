define([
    'flight/lib/component',
    'hbs!./templates/login'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(OAuthAuthentication);

    function OAuthAuthentication() {

        this.defaultAttrs({
            errorSelector: '.text-error',
        });

        this.after('initialize', function() {
            this.$node.html(template({}));
        });

    }
});
