package io.lumify.core.model.ontology;

public enum LabelName {
    HAS_PROPERTY("http://lumify.io/ontology#hasProperty"),
    HAS_EDGE("http://lumify.io/ontology#hasEdge"),
    IS_A("http://lumify.io/ontology#isA"),
    INVERSE_OF("http://lumify.io/ontology#inverseOf");

    private final String text;

    LabelName(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return this.text;
    }
}
