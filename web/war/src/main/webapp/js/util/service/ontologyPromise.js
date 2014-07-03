define(['service/ontology'], function(OntologyService) {
    'use strict';

    var ontologyService = new OntologyService();

    return ontologyService.ontology();
});
