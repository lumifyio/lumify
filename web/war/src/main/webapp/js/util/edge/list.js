define([
    'flight/lib/component'
], function(
    defineComponent) {
    'use strict';

    return defineComponent(EdgeList);

    function EdgeList() {

        this.after('initialize', function() {
            this.$node.html('Edges');
        });

    }
});
