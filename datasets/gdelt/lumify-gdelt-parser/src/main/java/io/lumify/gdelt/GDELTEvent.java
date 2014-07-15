package io.lumify.gdelt;

import java.util.Date;

public class GDELTEvent {
    // Event ID and Date Attributes
    private String globalEventId;
    private Date dateOfOccurrence;

    // Actor 1 Attributes
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

    // Actor 2 Attributes
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

    // Event Action Attributes
    private boolean isRootEvent;
    private String eventCode;
    private String eventBaseCode;
    private String eventRootCode;
    private int quadClass;
    private double goldsteinScale;
    private int numMentions;
    private int numSources;
    private int numArticles;
    private double averageTone;

    // Actor 1 Event Geography
    private int actor1GeoType;
    private String actor1GeoFullName;
    private String actor1GeoCountryCode;
    private String actor1GeoADM1Code;
    private double actor1GeoLatitude;
    private double actor1GeoLongitude;
    private String actor1GeoFeatureId;

    // Actor 2 Event Geography
    private int actor2GeoType;
    private String actor2GeoFullName;
    private String actor2GeoCountryCode;
    private String actor2GeoADM1Code;
    private double actor2GeoLatitude;
    private double actor2GeoLongitude;
    private String actor2GeoFeatureId;

    // Action Event Geography
    private int actionGeoType;
    private String actionGeoFullName;
    private String actionGeoCountryCode;
    private String actionGeoADM1Code;
    private double actionGeoLatitude;
    private double actionGeoLongitude;
    private String actionGeoFeatureId;

    // Date Management Fields
    private Date dateAdded;
    private String sourceUrl;

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

    @GDELTField(name = "IsRootEvent", required = true)
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

    @GDELTField(name = "QuadClass", required = true)
    public void setQuadClass(int quadClass) {
        this.quadClass = quadClass;
    }

    public double getGoldsteinScale() {
        return goldsteinScale;
    }

    @GDELTField(name = "GoldsteinScale")
    public void setGoldsteinScale(double goldsteinScale) {
        this.goldsteinScale = goldsteinScale;
    }

    public int getNumMentions() {
        return numMentions;
    }

    @GDELTField(name = "NumMentions")
    public void setNumMentions(int numMentions) {
        this.numMentions = numMentions;
    }

    public int getNumSources() {
        return numSources;
    }

    @GDELTField(name = "NumSources")
    public void setNumSources(int numSources) {
        this.numSources = numSources;
    }

    public int getNumArticles() {
        return numArticles;
    }

    @GDELTField(name = "NumArticles")
    public void setNumArticles(int numArticles) {
        this.numArticles = numArticles;
    }

    @GDELTField(name = "GLOBALEVENTID", required = true)
    public void setGlobalEventId(String globalEventId) {
        this.globalEventId = globalEventId;
    }

    public double getAverageTone() {
        return averageTone;
    }

    @GDELTField(name = "AvgTone")
    public void setAverageTone(double averageTone) {
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

    @GDELTField(name = "Actor1Geo_Type")
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

    public double getActor1GeoLatitude() {
        return actor1GeoLatitude;
    }

    @GDELTField(name = "Actor1Geo_Lat")
    public void setActor1GeoLatitude(double actor1GeoLatitude) {
        this.actor1GeoLatitude = actor1GeoLatitude;
    }

    public double getActor1GeoLongitude() {
        return actor1GeoLongitude;
    }

    @GDELTField(name = "Actor1Geo_Long")
    public void setActor1GeoLongitude(double actor1GeoLongitude) {
        this.actor1GeoLongitude = actor1GeoLongitude;
    }

    public String getActor1GeoFeatureId() {
        return actor1GeoFeatureId;
    }

    @GDELTField(name = "Actor1Geo_FeatureID")
    public void setActor1GeoFeatureId(String actor1GeoFeatureId) {
        this.actor1GeoFeatureId = actor1GeoFeatureId;
    }

    public int getActor2GeoType() {
        return actor2GeoType;
    }

    @GDELTField(name = "Actor2Geo_Type")
    public void setActor2GeoType(int actor2GeoType) {
        this.actor2GeoType = actor2GeoType;
    }

    public String getActor2GeoFullName() {
        return actor2GeoFullName;
    }

    @GDELTField(name = "Actor2Geo_FullName")
    public void setActor2GeoFullName(String actor2GeoFullName) {
        this.actor2GeoFullName = actor2GeoFullName;
    }

    public String getActor2GeoCountryCode() {
        return actor2GeoCountryCode;
    }

    @GDELTField(name = "Actor2Geo_CountryCode")
    public void setActor2GeoCountryCode(String actor2GeoCountryCode) {
        this.actor2GeoCountryCode = actor2GeoCountryCode;
    }

    public String getActor2GeoADM1Code() {
        return actor2GeoADM1Code;
    }

    @GDELTField(name = "Actor2Geo_ADM1Code")
    public void setActor2GeoADM1Code(String actor2GeoADM1Code) {
        this.actor2GeoADM1Code = actor2GeoADM1Code;
    }

    public double getActor2GeoLatitude() {
        return actor2GeoLatitude;
    }

    @GDELTField(name = "Actor2Geo_Lat")
    public void setActor2GeoLatitude(double actor2GeoLatitude) {
        this.actor2GeoLatitude = actor2GeoLatitude;
    }

    public double getActor2GeoLongitude() {
        return actor2GeoLongitude;
    }

    @GDELTField(name = "Actor2Geo_Long")
    public void setActor2GeoLongitude(double actor2GeoLongitude) {
        this.actor2GeoLongitude = actor2GeoLongitude;
    }

    public String getActor2GeoFeatureId() {
        return actor2GeoFeatureId;
    }

    @GDELTField(name = "Actor2Geo_FeatureID")
    public void setActor2GeoFeatureId(String actor2GeoFeatureId) {
        this.actor2GeoFeatureId = actor2GeoFeatureId;
    }

    public int getActionGeoType() {
        return actionGeoType;
    }

    @GDELTField(name = "ActionGeo_Type")
    public void setActionGeoType(int actionGeoType) {
        this.actionGeoType = actionGeoType;
    }

    public String getActionGeoFullName() {
        return actionGeoFullName;
    }

    @GDELTField(name = "ActionGeo_FullName")
    public void setActionGeoFullName(String actionGeoFullName) {
        this.actionGeoFullName = actionGeoFullName;
    }

    public String getActionGeoCountryCode() {
        return actionGeoCountryCode;
    }

    @GDELTField(name = "ActionGeo_CountryCode")
    public void setActionGeoCountryCode(String actionGeoCountryCode) {
        this.actionGeoCountryCode = actionGeoCountryCode;
    }

    public String getActionGeoADM1Code() {
        return actionGeoADM1Code;
    }

    @GDELTField(name = "ActionGeo_ADM1Code")
    public void setActionGeoADM1Code(String actionGeoADM1Code) {
        this.actionGeoADM1Code = actionGeoADM1Code;
    }

    public double getActionGeoLatitude() {
        return actionGeoLatitude;
    }

    @GDELTField(name = "ActionGeo_Lat")
    public void setActionGeoLatitude(double actionGeoLatitude) {
        this.actionGeoLatitude = actionGeoLatitude;
    }

    public double getActionGeoLongitude() {
        return actionGeoLongitude;
    }

    @GDELTField(name = "ActionGeo_Long")
    public void setActionGeoLongitude(double actionGeoLongitude) {
        this.actionGeoLongitude = actionGeoLongitude;
    }

    public String getActionGeoFeatureId() {
        return actionGeoFeatureId;
    }

    @GDELTField(name = "ActionGeo_FeatureID")
    public void setActionGeoFeatureId(String actionGeoFeatureId) {
        this.actionGeoFeatureId = actionGeoFeatureId;
    }

    public Date getDateAdded() {
        return dateAdded;
    }

    @GDELTField(name = "DATEADDED", dateFormat = "yyyyMMdd")
    public void setDateAdded(Date dateAdded) {
        this.dateAdded = dateAdded;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    @GDELTField(name = "SOURCEURL")
    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public GDELTActor getActor1() {
        return new GDELTActor(
                this.getActor1Code(),
                this.getActor1Name(),
                this.getActor1CountryCode(),
                this.getActor1KnownGroupCode(),
                this.getActor1EthnicCode(),
                this.getActor1Religion1Code(),
                this.getActor1Religion2Code(),
                this.getActor1Type1Code(),
                this.getActor1Type2Code(),
                this.getActor1Type3Code());
    }

    public GDELTActor getActor2() {
        return new GDELTActor(
                this.getActor2Code(),
                this.getActor2Name(),
                this.getActor2CountryCode(),
                this.getActor2KnownGroupCode(),
                this.getActor2EthnicCode(),
                this.getActor2Religion1Code(),
                this.getActor2Religion2Code(),
                this.getActor2Type1Code(),
                this.getActor2Type2Code(),
                this.getActor2Type3Code());
    }
}
