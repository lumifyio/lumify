
define([], function() {

    var nodeIdIncrement = 1;

    function Node(nodeId) {
        this.id = nodeId || ('_internalId' + nodeIdIncrement++);
        this.connections = [];
        this.data = {};
    }

    Node.prototype.isConnected = function(otherNode) {
        return this.connections.indexOf(otherNode) >= 0;
    };

    Node.prototype.connect = function(otherNode, options) {
        options = options || {};

        if (!this.isConnected(otherNode)) {
            this.connections.push(otherNode);
        }

        if (options.reverse) {
            otherNode.connect(this);
        }

        return this;
    };

    return Node;
});
