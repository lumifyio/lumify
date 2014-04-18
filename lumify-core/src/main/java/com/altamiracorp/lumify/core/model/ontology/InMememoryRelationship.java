package com.altamiracorp.lumify.core.model.ontology;

public class InMememoryRelationship extends Relationship {
    private String relationshipIRI;
    private String displayName;

    protected InMememoryRelationship(String relationshipIRI, String displayName, String sourceConceptIRI, String destConceptIRI) {
        super(sourceConceptIRI, destConceptIRI);
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
}
