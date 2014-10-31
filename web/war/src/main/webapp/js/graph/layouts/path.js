
define([
    'pathfinding',
    'util/retina',
    'util/formatters'
], function(PF, retina, F) {

    var DEBUG_SHOW_GRID = false;

    return doLayout;

    function doLayout(cy, currentNodes, boundingBox, vertexIds, layoutOptions) {

        var layoutPositions = {},
            // Hard coded for best "feel" bigger then icon, but
            // smaller then text label
            cell = { x: 150, y: 75 },
            start = retina.pixelsToPoints({x: boundingBox.x1, y: boundingBox.y1}),
            size = retina.pixelsToPoints({x: boundingBox.w,y: boundingBox.h}),
            outerCellPadding = 2;

        // Add 2 cells of padding cells around box
        start.x = Math.ceil(start.x - cell.x * outerCellPadding);
        start.y = Math.ceil(start.y - cell.y * outerCellPadding);
        size.x = Math.ceil(size.x + cell.x * (outerCellPadding * 2));
        size.y = Math.ceil(size.y + cell.y * (outerCellPadding * 2));

        var numCellsX = Math.ceil(size.x / cell.x),
            numCellsY = Math.ceil(size.y / cell.y),
            grid = new PF.Grid(numCellsX, numCellsY),
            finder = new PF.AStarFinder({
                allowDiagonal: true,
                dontCrossCorners: false
            }),
            nodesOnGraph = currentNodes,
            previouslyPlaced = [],
            previouslyPlacedMap = {};

        // Sort vertex dependencies
        vertexIds.sort(function(vId) {
            return _.uniq(
                _.pluck(layoutOptions.map, 'sourceId')
                    .concat(_.pluck(layoutOptions.map, 'targetId'))
            ).indexOf(vId) * -1;
        })

        // Position vertices
        vertexIds.forEach(function(vId) {
            if (layoutPositions[vId]) return;

            var pathVertices = layoutOptions.map[vId] || {},
            sourcePosition, targetPosition,
            clonedGrid = grid.clone();

            previouslyPlaced.forEach(function(p) {
                clonedGrid.setWalkableAt(p[0], p[1], false);
            });

            // Bin nodes already on graph into grid
            nodesOnGraph.each(function() {
                var position = retina.pixelsToPoints(this.position()),
                    nodeBoundingBox = this.boundingBox(),
                    nodeSize = retina.pixelsToPoints({ x: this.width(), y: this.height() }),
                    minX = Math.floor((position.x - start.x - nodeSize.x / 2) / cell.x),
                    maxX = Math.floor((position.x - start.x + nodeSize.x / 2) / cell.x),
                    minY = Math.floor((position.y - start.y - nodeSize.y / 2) / cell.y),
                    maxY = Math.floor(
                        (position.y - start.y +
                         nodeSize.y / 2 +
                         (nodeBoundingBox.h - nodeSize.y)
                        ) / cell.y
                    ),
                    x = Math.floor((position.x - start.x) / cell.x),
                    y = Math.floor((position.y - start.y) / cell.y),
                    nodeId = F.className.from(this.id());

                previouslyPlacedMap[nodeId] = {x: x,y: y};

                // Mark grid cells that shouldn't be used in path
                for (var wx = minX; wx <= maxX; wx++) {
                    for (var wy = minY; wy <= maxY; wy++) {
                        clonedGrid.setWalkableAt(wx, wy, false);
                    }
                }

                if (nodeId === pathVertices.sourceId) {
                    sourcePosition = {x: x, y: y};
                } else if (nodeId === pathVertices.targetId) {
                    targetPosition = {x: x,y: y};
                }
            });

            // Show grid overlay
            if (DEBUG_SHOW_GRID) {
                require(['graph/layouts/pathDebugGrid'], function(init) {
                    init(cy, cell);
                });
            }

            var findPosition = function(position) {
                var p = previouslyPlacedMap[pathVertices[position]];
                if (!p) {
                    var thisVertexEdges,
                        checkId = vId;
                    do {
                        thisVertexEdges = layoutOptions.map[checkId];
                        checkId = thisVertexEdges && thisVertexEdges[position];
                    } while (checkId && !previouslyPlacedMap[checkId]);

                    p = previouslyPlacedMap[checkId];
                }

                if (position === 'sourceId') {
                    sourcePosition = p;
                } else {
                    targetPosition = p;
                }
            };
            if (!sourcePosition) findPosition('sourceId');
            if (!targetPosition) findPosition('targetId');

            if (sourcePosition && targetPosition) {

                // Path won't work unless source and target are walkable
                clonedGrid.setWalkableAt(sourcePosition.x, sourcePosition.y, true);
                clonedGrid.setWalkableAt(targetPosition.x, targetPosition.y, true);

                var path = finder.findPath(
                    sourcePosition.x, sourcePosition.y,
                    targetPosition.x, targetPosition.y,
                    clonedGrid),
                    placeOnPath = [vId];

                    if (path.length) {

                        var middleIndex = Math.floor(path.length / 2),
                            targetNode = clonedGrid.getNodeAt(targetPosition.x, targetPosition.y),
                            position = path[middleIndex],
                            node = clonedGrid.getNodeAt(position[0], position[1]),
                            neighbors = clonedGrid.getNeighbors(node, true, false),
                            tryNodes = [node].concat(neighbors),
                            i = 0;

                        // Move position if we are on source/target cell by looking at neighbors
                        do {
                            node = tryNodes[i++];
                            position = [ node.x, node.y ];
                        } while (
                            i < tryNodes.length &&
                            (
                                (sourcePosition.x === position[0] && sourcePosition.y === position[1]) ||
                                (targetPosition.x === position[0] && targetPosition.y === position[1])
                            )
                        )

                        previouslyPlaced.push(position);
                        previouslyPlacedMap[vId] = {x: position[0], y: position[1] };
                        layoutPositions[vId] = {
                            x: position[0] * cell.x + start.x + (cell.x / 2),
                            y: position[1] * cell.y + start.y + (cell.y / 2)
                        };
                    }
            }
        });

        return layoutPositions;
    }
})
