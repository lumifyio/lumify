package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(DateUtil.class);

    public static Date extractDateTakenFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            JSONObject tagsObject = formatObject.optJSONObject("tags");
            if (tagsObject != null) {
                String dateTaken = null;
                String optionalDateTaken = tagsObject.optString("date");
                if (!optionalDateTaken.equals("")) {
                    dateTaken = optionalDateTaken;
                } else {
                    String optionalDateTakenEng = tagsObject.optString("date-eng");
                    if (!optionalDateTakenEng.equals("")) {
                        dateTaken = optionalDateTakenEng;
                    }
                }

                if (!dateTaken.equals("")) {
                    Date date = parseDateTakenString(dateTaken);
                    if (date != null) {
                        return date;
                    }
                }
            }
        }

        LOGGER.debug("Could not extract dateTaken from json.");
        return null;
    }

    public static Date extractLastModifyDateFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONArray streamsJson = json.optJSONArray("streams");
        if (streamsJson != null) {
            for (int i = 0; i < streamsJson.length(); i++) {
                JSONObject streamsIndexJson = streamsJson.optJSONObject(i);
                if (streamsIndexJson != null) {
                    JSONObject tagsJson = streamsIndexJson.optJSONObject("tags");
                    if (tagsJson != null) {
                        String creationTime = tagsJson.optString("creation_time");
                        if (!creationTime.equals("")) {
                            Date date = parseLastModifyDateString(creationTime);
                            if (date != null) {
                                return date;
                            }
                        }
                    }

                }
            }
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            JSONObject tagsObject = formatObject.optJSONObject("tags");
            if (tagsObject != null) {
                String creationTime = tagsObject.optString("creation_time");
                if (!creationTime.equals("")) {
                    Date date = parseLastModifyDateString(creationTime);
                    if (date != null) {
                        return date;
                    }
                }
            }
        }

        LOGGER.debug("Could not extract lastModifyDate (creation_time) from json.");
        return null;
    }

    private static Date parseDateTakenString(String dateTaken) {
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        try {
            Date parsedDate = format.parse(dateTaken);
            return parsedDate;
        } catch (ParseException e) {
            LOGGER.error("ParseException: could not parse dateTaken: " + dateTaken + " with date format: " + dateFormat);
        }

        return null;
    }

    private static Date parseLastModifyDateString(String lastModifyDate) {
        String dateFormat = "yyyy-MM-dd HH:mm:ssZ";
        String lastModifyDateInUTC = lastModifyDate + "-0000";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        try {
            Date parsedDate = format.parse(lastModifyDateInUTC);
            return parsedDate;
        } catch (ParseException e) {
            LOGGER.error("ParseException: could not parse lastModifyDate: " + lastModifyDate + " with date format: " + dateFormat);
        }

        return null;
    }

}
