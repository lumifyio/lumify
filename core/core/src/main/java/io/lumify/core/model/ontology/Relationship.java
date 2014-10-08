package io.lumify.core.model.ontology;

import io.lumify.web.clientapi.model.Ontology;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;

public abstract class Relationship {
    private final String sourceConceptIRI;
    private final String destConceptIRI;

    protected Relationship(String sourceConceptIRI, String destConceptIRI) {
        this.sourceConceptIRI = sourceConceptIRI;
        this.destConceptIRI = destConceptIRI;
    }

    public abstract String getIRI();

    public abstract String getDisplayName();

    public abstract Iterable<String> getInverseOfIRIs();

    public String getSourceConceptIRI() {
        return sourceConceptIRI;
    }

    public String getDestConceptIRI() {
        return destConceptIRI;
    }

    public Ontology.Relationship toClientApi() {
        try {
            Ontology.Relationship result = new Ontology.Relationship();
            result.setTitle(getIRI());
            result.setDisplayName(getDisplayName());
            result.setSource(getSourceConceptIRI());
            result.setDest(getDestConceptIRI());

            Iterable<String> inverseOfIRIs = getInverseOfIRIs();
            for (String inverseOfIRI : inverseOfIRIs) {
                Ontology.Relationship.InverseOf inverseOf = new Ontology.Relationship.InverseOf();
                inverseOf.setIri(inverseOfIRI);
                inverseOf.setPrimaryIri(getPrimaryInverseOfIRI(getIRI(), inverseOfIRI));
                result.getInverseOfs().add(inverseOf);
            }

            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getPrimaryInverseOfIRI(String iri1, String iri2) {
        if (iri1.compareTo(iri2) > 0) {
            return iri2;
        }
        return iri1;
    }

    public static Collection<Ontology.Relationship> toClientApiRelationships(Iterable<Relationship> relationships) {
        Collection<Ontology.Relationship> results = new ArrayList<Ontology.Relationship>();
        for (Relationship vertex : relationships) {
            results.add(vertex.toClientApi());
        }
        return results;
    }
}
