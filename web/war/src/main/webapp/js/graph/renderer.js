
define([
    'cytoscape',
    './shapes/movieStrip'
], function(cytoscape, movieStrip) {
    'use strict';

    cytoscape.style.types.nodeShape.enums.push('none');
    cytoscape.style.types.nodeShape.enums.push('movieStrip');

    var CanvasRenderer = cytoscape.extension('renderer', 'canvas'),
        nodeShapes = CanvasRenderer.nodeShapes;

    function Renderer(options) {
        CanvasRenderer.call(this, options);
    }

    var drawInscribedImage = CanvasRenderer.prototype.drawInscribedImage;

    Renderer.prototype = CanvasRenderer.prototype;

    Renderer.prototype.getNodeShape = function(node)
	{
		var shape = node._private.style.shape.value;

        if (shape == 'none') return 'ellipse';

		if (node.isParent()) {
			if (shape === 'rectangle' || shape === 'roundrectangle') {
				return shape;
			} else {
				return 'rectangle';
			}
		}

		return shape;
	};

    /**
     * Scale image to the size of the node
     */
	Renderer.prototype.drawInscribedImage = function(context, img, node) {
		var self = this,
            zoom = this.data.cy._private.zoom,
            nodeX = node._private.position.x,
            nodeY = node._private.position.y,
            nodeWidth = this.getNodeWidth(node),
            nodeHeight = this.getNodeHeight(node),
            ratioImage = img.width / img.height,
            ratioNode = nodeWidth / nodeHeight,
            fitW,
            fitH;

        // Fit inside node preserving aspect ratio of original image
        if (ratioNode > ratioImage) {
            fitW = img.width * nodeHeight / img.height;
            fitH = nodeHeight;
        } else {
            fitW = nodeWidth;
            fitH = img.height * nodeWidth / img.width;
        }

		context.save();
        context.globalAlpha = node._private.style.opacity.value;

        if (img.nodeName === 'IMG' && (!img.naturalWidth || !img.naturalHeight)) {
            drawError(context, nodeX, nodeY, nodeWidth, nodeHeight);
        } else {

            // Draw outline and clip to it based on node shape css
            if (node._private.style.shape.value !== 'none') {
                var shapeName = self.getNodeShape(node),
                    shape = nodeShapes[shapeName];

                if (shape && shape.drawBackground) {
                    context.save();
                    context.fillStyle = context.strokeStyle;
                    shape.drawBackground(context, nodeX, nodeY, fitW, fitH);
                    context.restore();
                }
                shape.drawPath(context, nodeX, nodeY, fitW, fitH);
                context.clip();
            }

            context.drawImage(img,
                    nodeX - fitW / 2,
                    nodeY - fitH / 2,
                    fitW,
                    fitH);
        }

		context.restore();
	};

    return Renderer;

    function drawError(context, nodeX, nodeY, nodeWidth, nodeHeight) {
        var lineWidth = Math.round(nodeWidth * 0.01),
            minDim = Math.round(Math.min(nodeWidth, nodeHeight) * 0.75 / 2);
        nodeWidth = nodeHeight = minDim;

        // Draw circle background
        context.beginPath();
        context.arc(
            nodeX,
            nodeY,
            minDim,
            2 * Math.PI,
            false
        );
        context.fillStyle = '#FFF9F9';
        context.strokeStyle = '#AD8E8E';
        context.lineWidth = lineWidth;
        context.fill();
        context.strokeWidth = Math.round(nodeWidth * 0.1);
        context.stroke();

        var padding = Math.round(nodeHeight * 0.1),
            errorWidth = Math.round(nodeWidth * 0.2),
            errorHeight = Math.round(nodeWidth * 0.8),
            errorBottomHeight = Math.round(nodeWidth * 0.2);

        // Top of exclamation
        context.beginPath();
        context.translate(
            nodeX - errorWidth / 2,
            nodeY - (errorHeight + errorBottomHeight + padding) / 2
        );
        context.rect(
            0, 0,
            errorWidth,
            errorHeight
        );
        context.fillStyle = context.strokeStyle;
        context.fill();

        // Bottom of exclamation
        context.beginPath();
        context.rect(
            0, errorHeight + padding,
            errorWidth,
            errorBottomHeight
        );
        context.fill();
    }
});
