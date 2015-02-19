package io.lumify.core.model.ontology;

import io.lumify.web.clientapi.model.ClientApiOntology;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public abstract class Relationship {
    private final List<String> domainConceptIRIs;
    private final List<String> rangeConceptIRIs;

    protected Relationship(List<String> domainConceptIRIs, List<String> rangeConceptIRIs) {
        this.domainConceptIRIs = domainConceptIRIs;
        this.rangeConceptIRIs = rangeConceptIRIs;
    }

    public abstract String getIRI();

    public abstract String getDisplayName();

    public abstract Iterable<String> getInverseOfIRIs();

    public List<String> getDomainConceptIRIs() {
        return domainConceptIRIs;
    }

    public List<String> getRangeConceptIRIs() {
        return rangeConceptIRIs;
    }

    public abstract boolean getUserVisible();

    public abstract String[] getIntents();

    public ClientApiOntology.Relationship toClientApi() {
        try {
            ClientApiOntology.Relationship result = new ClientApiOntology.Relationship();
            result.setTitle(getIRI());
            result.setDisplayName(getDisplayName());
            result.setDomainConceptIris(getDomainConceptIRIs());
            result.setRangeConceptIris(getRangeConceptIRIs());
            result.setUserVisible(getUserVisible());
            if (getIntents() != null) {
                result.getIntents().addAll(Arrays.asList(getIntents()));
            }

            Iterable<String> inverseOfIRIs = getInverseOfIRIs();
            for (String inverseOfIRI : inverseOfIRIs) {
                ClientApiOntology.Relationship.InverseOf inverseOf = new ClientApiOntology.Relationship.InverseOf();
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

    public static Collection<ClientApiOntology.Relationship> toClientApiRelationships(Iterable<Relationship> relationships) {
        Collection<ClientApiOntology.Relationship> results = new ArrayList<>();
        for (Relationship vertex : relationships) {
            results.add(vertex.toClientApi());
        }
        return results;
    }
}
