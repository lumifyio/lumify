package io.lumify.gdelt;

import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.properties.types.DateLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;

public class GDELTProperties {
    public static final StringLumifyProperty CONCEPT_TYPE = OntologyLumifyProperties.CONCEPT_TYPE;
    public static final StringLumifyProperty GLOBAL_EVENT_ID = new StringLumifyProperty("http://lumify.io/gdelt#globalEventId");
    public static final DateLumifyProperty EVENT_DATE = new DateLumifyProperty("http://lumify.io/gdelt#dateOfOccurrence");
}


