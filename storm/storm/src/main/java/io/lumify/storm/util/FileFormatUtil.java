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
            if (!optionalFileName.equals("")){

                /*Take the last characters of the filename (before the graphPropertyBolt number) as the file format.
                    and do not take more than 4 characters as the filename.
                 */
                int numberPosition = optionalFileName.length() - 1;
                for(int i = optionalFileName.length(); i > 0 && (i > optionalFileName.length() - 5 ); i--){
                    String oneCharacter = optionalFileName.substring(i - 1, i);
                    if (oneCharacter != null && isNumeric(oneCharacter) ){
                        numberPosition = i - 1;
                        String fileEnding = optionalFileName.substring(numberPosition + 1, optionalFileName.length());
                        return fileEnding;
                    }
                }
            }
        }

        LOGGER.debug("Could not extract file format from json.");
        return null;
    }

    private static boolean isNumeric(String inputString)
    {
        try
        {
            int temp = Integer.parseInt(inputString);
        }
        catch(NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
}
