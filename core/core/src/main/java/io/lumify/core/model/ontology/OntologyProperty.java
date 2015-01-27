package io.lumify.core.model.ontology;

import io.lumify.core.exception.LumifyException;
import io.lumify.web.clientapi.model.ClientApiOntology;
import io.lumify.web.clientapi.model.PropertyType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.securegraph.type.GeoCircle;
import org.securegraph.type.GeoPoint;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class OntologyProperty {
    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    public static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    public static final Pattern GEO_LOCATION_FORMAT = Pattern.compile("POINT\\((.*?),(.*?)\\)", Pattern.CASE_INSENSITIVE);
    public static final Pattern GEO_LOCATION_ALTERNATE_FORMAT = Pattern.compile("(.*?),(.*)", Pattern.CASE_INSENSITIVE);

    static {
        DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
        DATE_TIME_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public abstract String getTitle();

    public abstract String getDisplayName();

    public abstract boolean getUserVisible();

    public abstract boolean getSearchable();

    public abstract PropertyType getDataType();

    public abstract Double getBoost();

    public abstract Map<String, String> getPossibleValues();

    public abstract String getDisplayType();

    public abstract String getPropertyGroup();

    public abstract String[] getIntents();

    public static Collection<ClientApiOntology.Property> toClientApiProperties(Iterable<OntologyProperty> properties) {
        Collection<ClientApiOntology.Property> results = new ArrayList<ClientApiOntology.Property>();
        for (OntologyProperty property : properties) {
            results.add(property.toClientApi());
        }
        return results;
    }

    public ClientApiOntology.Property toClientApi() {
        try {
            ClientApiOntology.Property result = new ClientApiOntology.Property();
            result.setTitle(getTitle());
            result.setDisplayName(getDisplayName());
            result.setUserVisible(getUserVisible());
            result.setSearchable(getSearchable());
            result.setDataType(getDataType());
            result.setDisplayType(getDisplayType());
            result.setPropertyGroup(getPropertyGroup());
            if (getPossibleValues() != null) {
                result.getPossibleValues().putAll(getPossibleValues());
            }
            if (getIntents() != null) {
                result.getIntents().addAll(Arrays.asList(getIntents()));
            }
            return result;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public Object convertString(String valueStr) throws ParseException {
        PropertyType dataType = getDataType();
        Object value = valueStr;
        switch (dataType) {
            case DATE:
                try {
                    value = DATE_TIME_FORMAT.parse(valueStr);
                } catch (ParseException ex) {
                    value = DATE_FORMAT.parse(valueStr);
                }
                break;
            case GEO_LOCATION:
                value = parseGeoLocation(valueStr);
                break;
            case CURRENCY:
                value = new BigDecimal(valueStr);
                break;
            case DOUBLE:
                value = Double.parseDouble(valueStr);
                break;
            case INTEGER:
                value = Integer.parseInt(valueStr);
                break;
            case BOOLEAN:
                value = Boolean.parseBoolean(valueStr);
                break;
        }
        return value;
    }

    public static Object convert(JSONArray values, PropertyType propertyDataType, int index) throws ParseException {
        switch (propertyDataType) {
            case DATE:
                String valueStr = values.getString(index);
                try {
                    return DATE_TIME_FORMAT.parse(valueStr);
                } catch (ParseException ex) {
                    return DATE_FORMAT.parse(valueStr);
                }
            case GEO_LOCATION:
                return new GeoCircle(
                        values.getDouble(index),
                        values.getDouble(index + 1),
                        values.getDouble(index + 2)
                );
            case CURRENCY:
                return new BigDecimal(values.getString(index));
            case INTEGER:
                return values.getInt(index);
            case DOUBLE:
                return values.getDouble(index);
            case BOOLEAN:
                return values.getBoolean(index);
        }
        return values.getString(index);
    }

    protected static Object parseGeoLocation(String valueStr) {
        try {
            JSONObject json = new JSONObject(valueStr);
            double latitude = json.getDouble("latitude");
            double longitude = json.getDouble("longitude");
            String altitudeString = json.optString("altitude");
            Double altitude = (altitudeString == null || altitudeString.length() == 0) ? null : Double.parseDouble(altitudeString);
            String description = json.optString("description");
            return new GeoPoint(latitude, longitude, altitude, description);
        } catch (Exception ex) {
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
            throw new LumifyException("Could not parse location: " + valueStr);
        }
    }
}
