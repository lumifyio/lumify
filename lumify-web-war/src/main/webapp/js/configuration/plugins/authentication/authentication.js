define([
    'flight/lib/component'
], function(
    defineComponent) {
    'use strict';

    return defineComponent(AuthenticationMissing);

    function AuthenticationMissing() {

        this.after('initialize', function() {
            this.$node.text('Missing authenication plugin');
        });

    }
});
