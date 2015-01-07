package io.lumify.core.model.ontology;

import org.securegraph.util.ConvertingIterable;

import java.util.ArrayList;
import java.util.List;

public class InMemoryRelationship extends Relationship {
    private String relationshipIRI;
    private String displayName;
    private List<Relationship> inverseOfs = new ArrayList<Relationship>();

    protected InMemoryRelationship(String relationshipIRI, String displayName, List<String> domainConceptIRIs, List<String> rangeConceptIRIs) {
        super(domainConceptIRIs, rangeConceptIRIs);
        this.relationshipIRI = relationshipIRI;
        this.displayName = displayName;
    }

    @Override
    public String getIRI() {
        return relationshipIRI;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public Iterable<String> getInverseOfIRIs() {
        return new ConvertingIterable<Relationship, String>(inverseOfs) {
            @Override
            protected String convert(Relationship o) {
                return o.getIRI();
            }
        };
    }

    public void addInverseOf(Relationship inverseOfRelationship) {
        inverseOfs.add(inverseOfRelationship);
    }
}
