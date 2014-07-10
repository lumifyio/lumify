package io.lumify.gdelt;

import java.util.Date;

public class GDELTEvent {
    private String globalEventId;
    private Date dateOfOccurrence;

    private String actor1Code;
    private String actor1Name;
    private String actor1CountryCode;
    private String actor1KnownGroupCode;
    private String actor1EthnicCode;
    private String actor1Religion1Code;
    private String actor1Religion2Code;
    private String actor1Type1Code;
    private String actor1Type2Code;
    private String actor1Type3Code;

    private String actor2Code;
    private String actor2Name;
    private String actor2CountryCode;
    private String actor2KnownGroupCode;
    private String actor2EthnicCode;
    private String actor2Religion1Code;
    private String actor2Religion2Code;
    private String actor2Type1Code;
    private String actor2Type2Code;
    private String actor2Type3Code;

    private boolean isRootEvent;
    private String eventCode;
    private String eventBaseCode;
    private String eventRootCode;
    private int quadClass;
    private float goldsteinScale;
    private int numMentions;
    private int numSources;
    private int numArticles;
    private float averageTone;

    private int actor1GeoType;
    private String actor1GeoFullName;
    private String actor1GeoCountryCode;
    private String actor1GeoADM1Code;
    private float actor1GeoLatitude;
    private float actor1GeoLongitude;
    private int actor1GeoFeatureId;

    public String getGlobalEventId() {
        return globalEventId;
    }

    public Date getDateOfOccurrence() {
        return dateOfOccurrence;
    }

    @GDELTField(name = "SQLDATE", required = true, dateFormat = "yyyyMMdd")
    public void setDateOfOccurrence(Date dateOfOccurrence) {
        this.dateOfOccurrence = dateOfOccurrence;
    }
    public boolean isRootEvent() {
        return isRootEvent;
    }
    @GDELTField(name="IsRootEvent", required = true, type = Boolean.class)
    public void setRootEvent(boolean isRootEvent) {
        this.isRootEvent = isRootEvent;
    }
    public String getEventCode() {
        return eventCode;
    }
    @GDELTField(name = "EventCode", required = true)
    public void setEventCode(String eventCode) {
        this.eventCode = eventCode;
    }
    public String getEventBaseCode() {
        return eventBaseCode;
    }

    @GDELTField(name = "EventBaseCode", required = true)
    public void setEventBaseCode(String eventBaseCode) {
        this.eventBaseCode = eventBaseCode;
    }

    public String getEventRootCode() {
        return eventRootCode;
    }

    @GDELTField(name = "EventRootCode", required = true)
    public void setEventRootCode(String eventRootCode) {
        this.eventRootCode = eventRootCode;
    }

    public int getQuadClass() {
        return quadClass;
    }

    @GDELTField(name = "QuadClass", type = Integer.class, required = true)
    public void setQuadClass(int quadClass) {
        this.quadClass = quadClass;
    }

    public float getGoldsteinScale() {
        return goldsteinScale;
    }

    @GDELTField(name = "GoldsteinScale", type = Float.class, required = true)
    public void setGoldsteinScale(float goldsteinScale) {
        this.goldsteinScale = goldsteinScale;
    }

    public int getNumMentions() {
        return numMentions;
    }

    @GDELTField(name = "NumMentions", type = Integer.class)
    public void setNumMentions(int numMentions) {
        this.numMentions = numMentions;
    }

    public int getNumSources() {
        return numSources;
    }

    @GDELTField(name = "NumSources", type = Integer.class)
    public void setNumSources(int numSources) {
        this.numSources = numSources;
    }

    public int getNumArticles() {
        return numArticles;
    }

    @GDELTField(name = "NumArticles", type = Integer.class)
    public void setNumArticles(int numArticles) {
        this.numArticles = numArticles;
    }

    @GDELTField(name = "GLOBALEVENTID", required = true)
    public void setGlobalEventId(String globalEventId) {
        this.globalEventId = globalEventId;
    }

    public float getAverageTone() {
        return averageTone;
    }

    @GDELTField(name = "AvgTone", type = Float.class)
    public void setAverageTone(float averageTone) {
        this.averageTone = averageTone;
    }

    public String getActor1Code() {
        return actor1Code;
    }

    @GDELTField(name = "Actor1Code")
    public void setActor1Code(String actor1Code) {
        this.actor1Code = actor1Code;
    }

    public String getActor1Name() {
        return actor1Name;
    }

    @GDELTField(name = "Actor1Name")
    public void setActor1Name(String actor1Name) {
        this.actor1Name = actor1Name;
    }

    public String getActor1CountryCode() {
        return actor1CountryCode;
    }

    @GDELTField(name = "Actor1CountryCode")
    public void setActor1CountryCode(String actor1CountryCode) {
        this.actor1CountryCode = actor1CountryCode;
    }

    public String getActor1KnownGroupCode() {
        return actor1KnownGroupCode;
    }

    @GDELTField(name = "Actor1KnownGroupCode")
    public void setActor1KnownGroupCode(String actor1KnownGroupCode) {
        this.actor1KnownGroupCode = actor1KnownGroupCode;
    }

    public String getActor1EthnicCode() {
        return actor1EthnicCode;
    }

    @GDELTField(name = "Actor1EthnicCode")
    public void setActor1EthnicCode(String actor1EthnicCode) {
        this.actor1EthnicCode = actor1EthnicCode;
    }

    public String getActor1Religion1Code() {
        return actor1Religion1Code;
    }

    @GDELTField(name = "Actor1Religion1Code")
    public void setActor1Religion1Code(String actor1Religion1Code) {
        this.actor1Religion1Code = actor1Religion1Code;
    }

    public String getActor1Religion2Code() {
        return actor1Religion2Code;
    }

    @GDELTField(name = "Actor1Religion2Code")
    public void setActor1Religion2Code(String actor1Religion2Code) {
        this.actor1Religion2Code = actor1Religion2Code;
    }

    public String getActor1Type1Code() {
        return actor1Type1Code;
    }

    @GDELTField(name = "Actor1Type1Code")
    public void setActor1Type1Code(String actor1Type1Code) {
        this.actor1Type1Code = actor1Type1Code;
    }

    public String getActor1Type2Code() {
        return actor1Type2Code;
    }

    @GDELTField(name = "Actor1Type2Code")
    public void setActor1Type2Code(String actor1Type2Code) {
        this.actor1Type2Code = actor1Type2Code;
    }

    public String getActor1Type3Code() {
        return actor1Type3Code;
    }

    @GDELTField(name = "Actor1Type3Code")
    public void setActor1Type3Code(String actor1Type3Code) {
        this.actor1Type3Code = actor1Type3Code;
    }

    public String getActor2Code() {
        return actor2Code;
    }

    @GDELTField(name = "Actor2Code")
    public void setActor2Code(String actor2Code) {
        this.actor2Code = actor2Code;
    }

    public String getActor2Name() {
        return actor2Name;
    }

    @GDELTField(name = "Actor2Name")
    public void setActor2Name(String actor2Name) {
        this.actor2Name = actor2Name;
    }

    public String getActor2CountryCode() {
        return actor2CountryCode;
    }

    @GDELTField(name = "Actor2CountryCode")
    public void setActor2CountryCode(String actor2CountryCode) {
        this.actor2CountryCode = actor2CountryCode;
    }

    public String getActor2KnownGroupCode() {
        return actor2KnownGroupCode;
    }

    @GDELTField(name = "Actor2KnownGroupCode")
    public void setActor2KnownGroupCode(String actor2KnownGroupCode) {
        this.actor2KnownGroupCode = actor2KnownGroupCode;
    }

    public String getActor2EthnicCode() {
        return actor2EthnicCode;
    }

    @GDELTField(name = "Actor2EthnicCode")
    public void setActor2EthnicCode(String actor2EthnicCode) {
        this.actor2EthnicCode = actor2EthnicCode;
    }

    public String getActor2Religion1Code() {
        return actor2Religion1Code;
    }

    @GDELTField(name = "Actor2Religion1Code")
    public void setActor2Religion1Code(String actor2Religion1Code) {
        this.actor2Religion1Code = actor2Religion1Code;
    }

    public String getActor2Religion2Code() {
        return actor2Religion2Code;
    }

    @GDELTField(name = "Actor2Religion2Code")
    public void setActor2Religion2Code(String actor2Religion2Code) {
        this.actor2Religion2Code = actor2Religion2Code;
    }

    public String getActor2Type1Code() {
        return actor2Type1Code;
    }

    @GDELTField(name = "Actor2Type1Code")
    public void setActor2Type1Code(String actor2Type1Code) {
        this.actor2Type1Code = actor2Type1Code;
    }

    public String getActor2Type2Code() {
        return actor2Type2Code;
    }

    @GDELTField(name = "Actor2Type2Code")
    public void setActor2Type2Code(String actor2Type2Code) {
        this.actor2Type2Code = actor2Type2Code;
    }

    public String getActor2Type3Code() {
        return actor2Type3Code;
    }

    @GDELTField(name = "Actor2Type3Code")
    public void setActor2Type3Code(String actor2Type3Code) {
        this.actor2Type3Code = actor2Type3Code;
    }

    public int getActor1GeoType() {
        return actor1GeoType;
    }

    @GDELTField(name = "Actor1Geo_Type", type = Integer.class)
    public void setActor1GeoType(int actor1GeoType) {
        this.actor1GeoType = actor1GeoType;
    }

    public String getActor1GeoFullName() {
        return actor1GeoFullName;
    }

    @GDELTField(name = "Actor1Geo_FullName")
    public void setActor1GeoFullName(String actor1GeoFullName) {
        this.actor1GeoFullName = actor1GeoFullName;
    }

    public String getActor1GeoCountryCode() {
        return actor1GeoCountryCode;
    }

    @GDELTField(name = "Actor1Geo_CountryCode")
    public void setActor1GeoCountryCode(String actor1GeoCountryCode) {
        this.actor1GeoCountryCode = actor1GeoCountryCode;
    }

    public String getActor1GeoADM1Code() {
        return actor1GeoADM1Code;
    }

    @GDELTField(name = "Actor1Geo_ADM1Code")
    public void setActor1GeoADM1Code(String actor1GeoADM1Code) {
        this.actor1GeoADM1Code = actor1GeoADM1Code;
    }

    public float getActor1GeoLatitude() {
        return actor1GeoLatitude;
    }

    @GDELTField(name = "Actor1Geo_Lat", type = Float.class)
    public void setActor1GeoLatitude(float actor1GeoLatitude) {
        this.actor1GeoLatitude = actor1GeoLatitude;
    }

    public float getActor1GeoLongitude() {
        return actor1GeoLongitude;
    }

    @GDELTField(name = "Actor1Geo_Long", type = Float.class)
    public void setActor1GeoLongitude(float actor1GeoLongitude) {
        this.actor1GeoLongitude = actor1GeoLongitude;
    }

    public int getActor1GeoFeatureId() {
        return actor1GeoFeatureId;
    }

    @GDELTField(name = "Actor1Geo_FeatureID", type = Integer.class)
    public void setActor1GeoFeatureId(int actor1GeoFeatureId) {
        this.actor1GeoFeatureId = actor1GeoFeatureId;
    }
}
