package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.DATA_TYPE;
import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.ONTOLOGY_TITLE;
import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.DISPLAY_TYPE;
import static com.altamiracorp.lumify.core.model.properties.LumifyProperties.DISPLAY_NAME;

public class OntologyProperty {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final Pattern GEO_LOCATION_FORMAT = Pattern.compile("POINT\\((.*?),(.*?)\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern GEO_LOCATION_ALTERNATE_FORMAT = Pattern.compile("(.*?),(.*)", Pattern.CASE_INSENSITIVE);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private final Vertex vertex;

    public OntologyProperty(Vertex vertex) {
        this.vertex = vertex;
    }

    public Object getId() {
        return this.vertex.getId();
    }

    public String getTitle() {
        return ONTOLOGY_TITLE.getPropertyValue(vertex);
    }

    public String getDisplayName() {
        return DISPLAY_NAME.getPropertyValue(vertex);
    }

    public String getDisplayType() {
        return DISPLAY_TYPE.getPropertyValue(vertex);
    }

    public PropertyType getDataType() {
        return PropertyType.convert(DATA_TYPE.getPropertyValue(vertex));
    }

    public Vertex getVertex() {
        return vertex;
    }

    public static JSONArray toJsonProperties(List<OntologyProperty> properties) {
        JSONArray json = new JSONArray();
        for (OntologyProperty property : properties) {
            json.put(property.toJson());
        }
        return json;
    }

    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("id", getId());
            json.put("title", getTitle());
            json.put("displayName", getDisplayName());
            json.put("displayType", getDisplayType());
            json.put("dataType", getDataType().toString());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Object convertString(String valueStr) throws ParseException {
        PropertyType dataType = getDataType();
        Object value = valueStr;
        switch (dataType) {
            case DATE:
                value = DATE_FORMAT.parse(valueStr);
                break;
            case GEO_LOCATION:
                value = parseGeoLocation(valueStr);
                break;
            case CURRENCY:
                value = Double.parseDouble(valueStr);
                break;
            case DOUBLE:
                value = Double.parseDouble(valueStr);
                break;
            case BOOLEAN:
                value = Boolean.parseBoolean(valueStr);
                break;
        }
        return value;
    }

    protected Object parseGeoLocation(String valueStr) {
        Matcher match = GEO_LOCATION_FORMAT.matcher(valueStr);
        if (match.find()) {
            double latitude = Double.parseDouble(match.group(1).trim());
            double longitude = Double.parseDouble(match.group(2).trim());
            return new GeoPoint(latitude, longitude);
        }
        match = GEO_LOCATION_ALTERNATE_FORMAT.matcher(valueStr);
        if (match.find()) {
            double latitude = Double.parseDouble(match.group(1).trim());
            double longitude = Double.parseDouble(match.group(2).trim());
            return new GeoPoint(latitude, longitude);
        }
        throw new RuntimeException("Could not parse location: " + valueStr);
    }
}
