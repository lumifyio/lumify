define([
    'flight/lib/component',
    'hbs!./userListTpl'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(UsersList);

    function UsersList() {

        this.after('initialize', function() {
            this.$node.html(template({}));
        });

    }
});
