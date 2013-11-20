
define( [], 
function() {

    var node_id_increment = 1;

    function Node( node_id ) {
        this.id = node_id || ('_internalId' + node_id_increment++);
        this.connections = [];
        //this.position = {};
        this.data = {};
        //this.layout = {};
    }

    Node.prototype.isConnected = function( otherNode ) {
        return this.connections.indexOf( otherNode ) >= 0;
    };

    Node.prototype.connect = function( otherNode, options ) {
        options = options || {};

        if ( ! this.isConnected( otherNode )) {
            this.connections.push( otherNode );
        }

        if ( options.reverse ) {
            otherNode.connect( this );
        }

        return this;
    };

    return Node;
});
