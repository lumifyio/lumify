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
            this.$node.html(template({}));
        });

    }
});
