define([], function() {
    'use strict';

    return withObjectsUpdated;

    function withObjectsUpdated() {

        this.storeObjectsUpdated = function(message) {
            if (message && message.vertices) {
                this.trigger(document, 'verticesUpdated', {
                    vertices: message.vertices
                });
            }
        }

    }
});
