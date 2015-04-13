package io.lumify.core.model.ontology;

import org.securegraph.util.ConvertingIterable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class InMemoryRelationship extends Relationship {
    private String relationshipIRI;
    private String displayName;
    private List<Relationship> inverseOfs = new ArrayList<>();
    private List<String> intents = new ArrayList<>();
    private boolean userVisible;

    protected InMemoryRelationship(String relationshipIRI, String displayName, List<String> domainConceptIRIs, List<String> rangeConceptIRIs, String[] intents, boolean userVisible) {
        super(domainConceptIRIs, rangeConceptIRIs);
        this.relationshipIRI = relationshipIRI;
        this.displayName = displayName;
        this.intents.addAll(Arrays.asList(intents));
        this.userVisible = userVisible;
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

    @Override
    public boolean getUserVisible() {
        return userVisible;
    }

    @Override
    public String[] getIntents() {
        return this.intents.toArray(new String[this.intents.size()]);
    }

    public void addInverseOf(Relationship inverseOfRelationship) {
        inverseOfs.add(inverseOfRelationship);
    }
}
