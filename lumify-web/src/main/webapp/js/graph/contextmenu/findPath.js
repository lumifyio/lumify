define([
    'util/retina',
], function (retina) {
    'use strict';

    return FindPath;

    function FindPath() {

        this.onContextMenuFindShortestPath = function (hops) {
            var menu = this.select('vertexContextMenuSelector');
            var graphVertexId = menu.data('currentVertexGraphVertexId');

            this.trigger('startVertexConnection', {
                sourceId: graphVertexId,
                connectionType: 'FindPath',
                hops: hops
            });
        };
    }
});
