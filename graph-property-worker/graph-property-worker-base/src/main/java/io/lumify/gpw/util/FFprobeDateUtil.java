package io.lumify.gpw.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FFprobeDateUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FFprobeDateUtil.class);

    public static Date getDateTaken(JSONObject json) {
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

                if (dateTaken != null && !dateTaken.equals("")) {
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

    private static Date parseDateTakenString(String dateTaken) {
        String dateFormat = "yyyy-MM-dd'T'HH:mm:ssZ";
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        try {
            Date parsedDate = format.parse(dateTaken);
            return parsedDate;
        } catch (ParseException e) {
            LOGGER.debug("ParseException: could not parse dateTaken: " + dateTaken + " with date format: " + dateFormat);
        }

        return null;
    }
}
