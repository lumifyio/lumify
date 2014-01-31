
define([
    './nodeShapes',
    'util/retina'
], function(nodeShapes, retina) {
    'use strict';

    var movieStrip = Object.create(nodeShapes.square);

    movieStrip.drawBackground = function(context, nodeX, nodeY, fitW, fitH) {
        var ratio = retina.devicePixelRatio,
            padding = 2 * ratio,
            total = 5,
            extend = 6 * ratio,
            height = Math.floor(fitH / total),
            holeSize = {
                width: extend * 0.6,
                height: height - padding * 2
            },
            holeLeftPosition = extend / 2 - holeSize.width / 2,
            leftPosition = {
                x: nodeX - fitW / 2 - extend,
                y: nodeY - fitH / 2 - padding
            };

        context.fillRect(leftPosition.x, leftPosition.y, fitW + extend * 2, fitH + padding * 2);
        context.fillStyle = 'white';

        for (var j = 0; j < 2; j++) {
            for (var i = 0; i < total; i++) {
                context.fillRect( 
                        leftPosition.x + holeLeftPosition + j * (fitW + extend),
                        leftPosition.y + i * height + padding * 2,
                        holeSize.width, 
                        holeSize.height);
            }
        }
    };

    nodeShapes.movieStrip = movieStrip;

    return movieStrip;
});
