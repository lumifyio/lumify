package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class Property extends GraphVertex {
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static Pattern GEO_LOCATION_FORMAT = Pattern.compile("POINT\\((.*?),(.*?)\\)", Pattern.CASE_INSENSITIVE);
    public static Pattern GEO_LOCATION_ALTERNATE_FORMAT = Pattern.compile("(.*?),(.*)", Pattern.CASE_INSENSITIVE);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public abstract String getId();

    public abstract String getTitle();

    public abstract String getDisplayName();

    public abstract PropertyType getDataType();

    public static JSONArray toJsonProperties(List<Property> properties) {
        JSONArray json = new JSONArray();
        for (Property property : properties) {
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
        }
        return value;
    }

    protected Object parseGeoLocation(String valueStr) {
        Matcher match = GEO_LOCATION_FORMAT.matcher(valueStr);
        if (match.find()) {
            double latitude = Double.parseDouble(match.group(1).trim());
            double longitude = Double.parseDouble(match.group(2).trim());
            return Geoshape.point(latitude, longitude);
        }
        match = GEO_LOCATION_ALTERNATE_FORMAT.matcher(valueStr);
        if (match.find()) {
            double latitude = Double.parseDouble(match.group(1).trim());
            double longitude = Double.parseDouble(match.group(2).trim());
            return Geoshape.point(latitude, longitude);
        }
        throw new RuntimeException("Could not parse location: " + valueStr);
    }
}
