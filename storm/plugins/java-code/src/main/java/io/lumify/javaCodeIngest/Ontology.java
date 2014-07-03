package io.lumify.javaCodeIngest;

import io.lumify.core.model.properties.types.StringLumifyProperty;

public class Ontology {
    public static final String EDGE_LABEL_JAR_CONTAINS = "http://lumify.io/java-code-ingest#jarFileContains";
    public static final String EDGE_LABEL_CLASS_FILE_CONTAINS_CLASS = "http://lumify.io/java-code-ingest#classFileContainsClass";
    public static final String EDGE_LABEL_CLASS_CONTAINS = "http://lumify.io/java-code-ingest#classContains";
    public static final String EDGE_LABEL_INVOKED = "http://lumify.io/java-code-ingest#invoked";
    public static final String EDGE_LABEL_FIELD_TYPE = "http://lumify.io/java-code-ingest#fieldType";
    public static final String EDGE_LABEL_METHOD_RETURN_TYPE = "http://lumify.io/java-code-ingest#methodReturnType";
    public static final String EDGE_LABEL_METHOD_ARGUMENT = "http://lumify.io/java-code-ingest#argument";
    public static final String EDGE_LABEL_CLASS_REFERENCES = "http://lumify.io/java-code-ingest#classReferences";
    public static final String CONCEPT_TYPE_JAR_FILE = "http://lumify.io/java-code-ingest#jarFile";
    public static final String CONCEPT_TYPE_CLASS_FILE = "http://lumify.io/java-code-ingest#classFile";
    public static final String CONCEPT_TYPE_CLASS = "http://lumify.io/java-code-ingest#class";
    public static final String CONCEPT_TYPE_INTERFACE = "http://lumify.io/java-code-ingest#interface";
    public static final String CONCEPT_TYPE_METHOD = "http://lumify.io/java-code-ingest#method";
    public static final String CONCEPT_TYPE_FIELD = "http://lumify.io/java-code-ingest#field";

    public static final StringLumifyProperty CLASS_NAME = new StringLumifyProperty("http://lumify.io/java-code-ingest#className");
    public static final StringLumifyProperty ARGUMENT_NAME = new StringLumifyProperty("http://lumify.io/java-code-ingest#argumentName");
}
