package com.altamiracorp.lumify.core.model.ontology;

import static com.altamiracorp.lumify.core.util.ObjectHelper.toStringOrNull;

import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.type.GeoPoint;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class OntologyProperty {
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static Pattern GEO_LOCATION_FORMAT = Pattern.compile("POINT\\((.*?),(.*?)\\)", Pattern.CASE_INSENSITIVE);
    public static Pattern GEO_LOCATION_ALTERNATE_FORMAT = Pattern.compile("(.*?),(.*)", Pattern.CASE_INSENSITIVE);

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
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0));
    }

    public String getDisplayName() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.DISPLAY_NAME.toString(), 0));
    }

    public String getDisplayType() {
        return toStringOrNull(this.vertex.getPropertyValue(PropertyName.DISPLAY_TYPE.toString(), 0));
    }

    public PropertyType getDataType() {
        return PropertyType.convert((toStringOrNull(this.vertex.getPropertyValue(PropertyName.DATA_TYPE.toString(), 0))));
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
        PropertyType dateType = getDataType();
        Object value = valueStr;
        switch (dateType) {
            case DATE:
                value = DATE_FORMAT.parse(valueStr).getTime();
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
