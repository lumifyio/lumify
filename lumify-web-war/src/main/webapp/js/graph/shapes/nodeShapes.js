
define([
    'cytoscape',
], function(cytoscape, movieStrip) {
    'use strict';

    var CanvasRenderer = cytoscape.extension( 'renderer', 'canvas' ),
        nodeShapes = CanvasRenderer.nodeShapes;

    return nodeShapes;
});
