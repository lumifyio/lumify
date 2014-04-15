package com.altamiracorp.lumify.core.model.ontology;

public class InMemRelationship extends Relationship {
    private String relationshipIRI;
    private String displayName;

    protected InMemRelationship(String relationshipIRI, String displayName, String sourceConceptIRI, String destConceptIRI) {
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
