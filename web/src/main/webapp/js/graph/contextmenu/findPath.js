define([
    'util/retina',
    'service/graph'
], function (retina, GraphService) {
    'use strict';

    return FindPath;

    function FindPath() {

        if (!this.graphService) {
            this.graphService = new GraphService();
        }

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
