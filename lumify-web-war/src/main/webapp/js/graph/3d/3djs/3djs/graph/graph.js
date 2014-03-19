
define(['./node'], function( Node ) {

    function Graph( options ) {
        this._options = options || {};
        this.nodeSet = {};
        this.nodes = [];
        this.edges = [];
    }

    Graph.Node = Node;

    Graph.prototype.node = function( idOrNode ) {
        var node = (typeof idOrNode === 'string' ) ? 
            this.nodeSet[ idOrNode ] : idOrNode;
        
        return node;
    };

    Graph.prototype.addNode = function( idOrNode ) {
        var node = (typeof idOrNode === 'string' || !idOrNode ) ? 
            new Node( idOrNode ) : idOrNode;

        if ( this.nodeSet[ node.id ] === undefined ) {

            this.nodeSet[ node.id ] = node;
            this.nodes.push(node);
        }

        return this;
    };

    Graph.prototype.removeNode = function( nodeId ) {
        var node = this.nodeSet[nodeId];

        if (node) {
            node.needsRemove = true;
        }
    };

    Graph.prototype.connect = function( node, nodeToConnect, options ) {
        node = this.node( node );
        nodeToConnect = this.node( nodeToConnect );

        if ( node && nodeToConnect ) {
            node.connect( nodeToConnect, options );

        } else throw new Error("Unable to connect nodes unless they both exist in the graph");

        return this;
    };

    return Graph;
});
