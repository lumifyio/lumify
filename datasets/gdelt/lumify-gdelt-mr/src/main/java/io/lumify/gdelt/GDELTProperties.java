package io.lumify.gdelt;

import io.lumify.core.model.ontology.OntologyLumifyProperties;
import io.lumify.core.model.properties.types.*;

public class GDELTProperties {
    public static final StringLumifyProperty CONCEPT_TYPE = OntologyLumifyProperties.CONCEPT_TYPE;
    public static final StringLumifyProperty GLOBAL_EVENT_ID = new StringLumifyProperty("http://lumify.io/gdelt#globalEventId");
    public static final DateLumifyProperty EVENT_DATE_OF_OCCURRENCE = new DateLumifyProperty("http://lumify.io/gdelt#dateOfOccurrence");
    public static final BooleanLumifyProperty EVENT_IS_ROOT_EVENT = new BooleanLumifyProperty("http://lumify.io/gdelt#isRootEvent");
    public static final StringLumifyProperty EVENT_CODE = new StringLumifyProperty("http://lumify.io/gdelt#eventCode");
    public static final StringLumifyProperty EVENT_BASE_CODE = new StringLumifyProperty("http://lumify.io/gdelt#eventBaseCode");
    public static final StringLumifyProperty EVENT_ROOT_CODE = new StringLumifyProperty("http://lumify.io/gdelt#eventRootCode");
    public static final IntegerLumifyProperty EVENT_QUAD_CLASS = new IntegerLumifyProperty("http://lumify.io/gdelt#quadClass");
    public static final DoubleLumifyProperty EVENT_GOLDSTEIN_SCALE = new DoubleLumifyProperty("http://lumify.io/gdelt#goldsteinScale");
    public static final IntegerLumifyProperty EVENT_NUM_MENTIONS = new IntegerLumifyProperty("http://lumify.io/gdelt#numMentions");
    public static final IntegerLumifyProperty EVENT_NUM_SOURCES = new IntegerLumifyProperty("http://lumify.io/gdelt#numSources");
    public static final IntegerLumifyProperty EVENT_NUM_ARTICLES = new IntegerLumifyProperty("http://lumify.io/gdelt#numArticles");
    public static final DoubleLumifyProperty EVENT_AVG_TONE = new DoubleLumifyProperty("http://lumify.io/gdelt#avgTone");
}


