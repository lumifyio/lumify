define([
    'flight/lib/component'
], function(
    defineComponent) {
    'use strict';

    return defineComponent(FindPath);

    function FindPath() {
        this.after('initialize', function() {
            this.$node.html('<button class="btn btn-mini btn-primary">Add Vertices</button>');
        });
    }
});
