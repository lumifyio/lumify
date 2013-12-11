define([
    'util/retina',
    'service/relationship',
    'service/ontology'
], function (retina, RelationshipService, OntologyService) {
    'use strict';

    return Connection;

    function Connection() {

        var input, edge;

        if (!this.relationshipService) {
            this.relationshipService = new RelationshipService();
            this.ontologyService = new OntologyService();
        }

        this.onContextMenuConnect = function () {
            var menu = this.select('vertexContextMenuSelector');
            var graphVertexId = menu.data('currentVertexGraphVertexId');

            this.trigger('startVertexConnection', {
                sourceId: graphVertexId,
                connectionType: 'CreateConnection'
            });
        };
    }
});
