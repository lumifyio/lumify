package io.lumify.web.clientapi.codegen;

import io.lumify.web.clientapi.codegen.model.Ontology;
import io.lumify.web.clientapi.codegen.model.OntologyConcept;

public class OntologyApiExt extends OntologyApi {
    private Ontology ontology;

    public OntologyConcept getConcept(String conceptIri) throws ApiException {
        if (ontology == null) {
            ontology = get();
        }
        for (OntologyConcept concept : ontology.getConcepts()) {
            if (concept.getId().equals(conceptIri)) {
                return concept;
            }
        }
        return null;
    }
}
