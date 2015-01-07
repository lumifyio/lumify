define([
    'flight/lib/component',
    'hbs!./template'
], function(
    defineComponent,
    template) {
    'use strict';

    return defineComponent(Access);

    function Access() {

        this.after('initialize', function() {
            var user = lumifyData.currentUser,
                order = 'EDIT COMMENT PUBLISH ADMIN'.split(' ');

            this.$node.html(template({
                privileges: _.chain(user.privileges)
                    .without('READ')
                    .sortBy(function(p) {
                        return order.indexOf(p);
                    })
                    .value(),
                authorizations: user.authorizations
            }));
        });

    }
});
