package io.lumify.storm.util;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.json.JSONObject;

public class FileFormatUtil {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(FileFormatUtil.class);

    public static String extractFileFormatFromJSON(JSONObject json) {
        if (json == null) {
            return null;
        }

        JSONObject formatObject = json.optJSONObject("format");
        if (formatObject != null) {
            String optionalFileName = formatObject.optString("filename");
            if (!optionalFileName.equals("")) {

                /*Take the last characters of the filename (before the graphPropertyBolt number) as the file format.
                    File format must start with a letter, and may be 3 or 4 characters. Ex: webm, mp4, mov, divx.
                 */
                if (optionalFileName.length() >= 4) {
                    String characterFour = optionalFileName.substring(optionalFileName.length() - 4, optionalFileName.length() - 3);
                    if (!isNumeric(characterFour)) {
                        String lastFourCharacters = optionalFileName.substring(optionalFileName.length() - 4, optionalFileName.length());
                        return lastFourCharacters;
                    }
                }

                if (optionalFileName.length() >= 3) {
                    String characterThree = optionalFileName.substring(optionalFileName.length() - 3, optionalFileName.length() - 2);
                    if (!isNumeric(characterThree)) {
                        String lastThreeCharacters = optionalFileName.substring(optionalFileName.length() - 3, optionalFileName.length());
                        return lastThreeCharacters;
                    }
                }
            }
        }

        LOGGER.debug("Could not extract file format from json.");
        return null;
    }

    private static boolean isNumeric(String inputString) {
        try {
            int temp = Integer.parseInt(inputString);
        } catch (NumberFormatException nfe) {
            return false;
        }
        return true;
    }
}
