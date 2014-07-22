package io.lumify.gdelt;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.properties.types.*;

public class GDELTProperties {
    public static final StringLumifyProperty CONCEPT_TYPE = LumifyProperties.CONCEPT_TYPE;

    // event properties
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
    public static final GeoPointLumifyProperty EVENT_GEOLOCATION = new GeoPointLumifyProperty("http://lumify.io/gdelt#geoLocation");
    public static final DateLumifyProperty EVENT_DATE_ADDED = new DateLumifyProperty("http://lumify.io/gdelt#dateAdded");
    public static final StringLumifyProperty EVENT_SOURCE_URL = new StringLumifyProperty("http://lumify.io/gdelt#sourceUrl");

    // actor properties
    public static final StringLumifyProperty ACTOR_CODE = new StringLumifyProperty("http://lumify.io/gdelt#actorCode");
    public static final StringLumifyProperty ACTOR_NAME = new StringLumifyProperty("http://lumify.io/gdelt#actorName");
    public static final StringLumifyProperty ACTOR_COUNTRY_CODE = new StringLumifyProperty("http://lumify.io/gdelt#countryCode");
    public static final StringLumifyProperty ACTOR_KNOWN_GROUP_CODE = new StringLumifyProperty("http://lumify.io/gdelt#knownGroupCode");
    public static final StringLumifyProperty ACTOR_ETHNIC_CODE = new StringLumifyProperty("http://lumify.io/gdelt#ethnicCode");
    public static final StringLumifyProperty ACTOR_RELIGION_CODE = new StringLumifyProperty("http://lumify.io/gdelt#religionCode");
    public static final StringLumifyProperty ACTOR_TYPE_CODE = new StringLumifyProperty("http://lumify.io/gdelt#typeCode");

    public static final String ACTOR1_TO_EVENT_EDGE = "http://lumify.io/gdelt#acted";
    public static final String EVENT_TO_ACTOR2_EDGE = "http://lumify.io/gdelt#wasActedUpon";
}


