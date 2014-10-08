package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.OntologyApi;
import io.lumify.web.clientapi.model.Ontology;

public class OntologyApiExt extends io.lumify.web.clientapi.codegen.OntologyApi {
    private Ontology ontology;

    public Ontology.Concept getConcept(String conceptIri) throws ApiException {
        if (ontology == null) {
            ontology = get();
        }
        for (Ontology.Concept concept : ontology.getConcepts()) {
            if (concept.getId().equals(conceptIri)) {
                return concept;
            }
        }
        return null;
    }
}
