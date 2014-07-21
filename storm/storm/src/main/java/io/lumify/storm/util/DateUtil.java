package io.lumify.storm.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class DateUtil {
    public static Date extractDateFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        try {
            JSONArray streamsJson = json.getJSONArray("streams");
            for (int i = 0; i < streamsJson.length(); i++) {
                try {
                    JSONObject streamsIndexJson = streamsJson.getJSONObject(i);
                    JSONObject tagsJson = streamsIndexJson.getJSONObject("tags");
                    String creationTime = tagsJson.getString("creation_time");
                    System.out.println(creationTime);
                    Date date = parseDateString(creationTime);
                    if (date != null) {
                        return date;
                    }
                } catch (JSONException e) {
                    //Could not find "creation_time" name on this pathway. Keep searching.
                }
            }
        } catch (Exception e) {
            System.out.println("Could not retrieve a \"geoLocation\" value from the JSON object.");
            e.printStackTrace();
        }

        try {
            JSONObject formatObject = json.getJSONObject("format");
            JSONObject tagsObject = formatObject.getJSONObject("tags");
            String creationTime = tagsObject.getString("creation_time");
            System.out.println(creationTime);
            Date date = parseDateString(creationTime);
            if (date != null) {
                return date;
            }
        } catch (Exception e) {
            //Could not find "creation_time" name on this pathway. Keep searching.
        }

        return null;
    }


    private static Date parseDateString(String dateString) {
        if (dateString.length() < 9) {
            return null;
        }
        Integer year = Integer.parseInt(dateString.substring(0, 4));
        Integer month = Integer.parseInt(dateString.substring(5,7));
        Integer day = Integer.parseInt(dateString.substring(8,10));

        Integer hour = null;
        Integer min = null;
        try {
            hour = Integer.parseInt(dateString.substring(11, 13));
            min = Integer.parseInt(dateString.substring(14, 16));
        } catch (Exception e){
            //No action required.
        }

        Integer sec = null;
        try {
            sec = Integer.parseInt(dateString.substring(17, 19));
        } catch (Exception e){
            //No action required.
        }

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
        cal.clear();
        if(year != null && month != null && day != null){
            if(hour != null && min != null && sec != null){
                cal.set(year,month,day,hour,min,sec);
            } else if (hour != null && min != null){
                cal.set(year,month,day,hour,min);
            } else {
                cal.set(year, month, day);
            }
            Date date = cal.getTime();
            return date;
        } else {
            return null;
        }
    }
}
