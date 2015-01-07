package io.lumify.gpw.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;
import org.securegraph.type.GeoPoint;

import java.util.ArrayList;

public class FFprobeGeoLocationUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FFprobeGeoLocationUtil.class);

    public static GeoPoint getGeoPoint(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            JSONObject tagsObject = formatObject.optJSONObject("tags");
            if (tagsObject != null) {
                String locationString = tagsObject.optString("location");
                if (!locationString.equals("")) {
                    GeoPoint geoPoint = parseGeoLocationString(locationString);
                    if (geoPoint != null) {
                        return geoPoint;
                    }
                }
            }
        }


        LOGGER.debug("Could not retrieve a \"geoLocation\" value from the JSON object.");
        return null;
    }

    private static GeoPoint parseGeoLocationString(String locationString) {
        String myRegularExpression = "(\\+|\\-|/)";
        String[] tempValues = locationString.split(myRegularExpression);
        String[] values = removeNullsFromStringArray(tempValues);
        if (values.length < 2) {
            return null;
        }

        String latitudeValue = values[0];
        String latitudeSign = "";
        int indexOfLatitude = locationString.indexOf(latitudeValue);
        if (indexOfLatitude != 0) {
            latitudeSign = locationString.substring(0, 1);
        }
        String latitudeString = latitudeSign + latitudeValue;
        Double latitude = Double.parseDouble(latitudeString);

        String longitudeValue = values[1];
        String longitudeSign = "";
        int indexOfLongitude = locationString.indexOf(longitudeValue, indexOfLatitude + latitudeValue.length());
        String longitudePreviousChar = locationString.substring(indexOfLongitude - 1, indexOfLongitude);
        if (longitudePreviousChar.equals("-") || longitudePreviousChar.equals("+")) {
            longitudeSign = longitudePreviousChar;
        }
        String longitudeString = longitudeSign + longitudeValue;
        Double longitude = Double.parseDouble(longitudeString);

        String altitudeValue = null;
        Double altitude = null;
        if (values.length == 3) {
            altitudeValue = values[2];
            String altitudeSign = "";
            int indexOfAltitude = locationString.indexOf(altitudeValue, indexOfLongitude + longitudeValue.length());
            String altitudePreviousChar = locationString.substring(indexOfAltitude - 1, indexOfAltitude);
            if (altitudePreviousChar.equals("-") || altitudePreviousChar.equals("+")) {
                altitudeSign = altitudePreviousChar;
            }
            String altitudeString = altitudeSign + altitudeValue;
            altitude = Double.parseDouble(altitudeString);
        }

        if (latitude != null && longitude != null && altitude != null) {
            return new GeoPoint(latitude, longitude, altitude);
        } else if (latitude != null && longitude != null && altitude == null) {
            return new GeoPoint(latitude, longitude);
        } else {
            return null;
        }
    }

    public static String[] removeNullsFromStringArray(String[] array) {
        ArrayList<String> arrayList = new ArrayList<String>();
        for (int i = 0; i < array.length; i++) {
            if (array[i] != null && !array[i].equals("")) {
                arrayList.add(array[i]);
            }
        }
        String[] newArray = arrayList.toArray(new String[arrayList.size()]);
        return newArray;
    }
}
