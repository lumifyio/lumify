package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.ClientApiOntology;

public class OntologyApiExt extends io.lumify.web.clientapi.codegen.OntologyApi {
    private ClientApiOntology ontology;

    public ClientApiOntology.Concept getConcept(String conceptIri) throws ApiException {
        if (ontology == null) {
            ontology = get();
        }
        for (ClientApiOntology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals(conceptIri)) {
                return concept;
            }
        }
        return null;
    }
}
