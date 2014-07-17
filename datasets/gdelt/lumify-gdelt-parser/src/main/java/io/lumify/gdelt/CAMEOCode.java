package io.lumify.gdelt;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class CAMEOCode {
    private static Map<String, String> actorCodes = new HashMap<String, String>();
    private static Map<String, String> eventCodes = new HashMap<String, String>();
    private static Map<String, String> goldsteinScores = new HashMap<String, String>();

    static {
        actorCodes.putAll(loadCodesFromFile("CAMEO.country.txt"));
        actorCodes.putAll(loadCodesFromFile("CAMEO.type.txt"));
        actorCodes.putAll(loadCodesFromFile("CAMEO.knowngroup.txt"));
        actorCodes.putAll(loadCodesFromFile("CAMEO.ethnic.txt"));
        actorCodes.putAll(loadCodesFromFile("CAMEO.religion.txt"));
        eventCodes.putAll(loadCodesFromFile("CAMEO.eventcodes.txt"));
        goldsteinScores.putAll(loadCodesFromFile("CAMEO.goldsteinscale.txt"));
    }

    public static String getActorDescription(String code) {
        code = code != null ? code.trim() : code;
        return actorCodes.get(code);
    }

    public static String getEventDescription(String code) {
        code = code != null ? code.trim() : code;
        return eventCodes.get(code);
    }

    public static String getGoldsteinScore(String code) {
        code = code != null ? code.trim() : code;
        return goldsteinScores.get(code);
    }

    private static Map<String, String> loadCodesFromFile(String fileName) {
        Map<String, String> lookupTable = new HashMap<String, String>();

        InputStream is = CAMEOCode.class.getResourceAsStream(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\\t");
                lookupTable.put(fields[0].trim(), fields[1].trim());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Failed to close reader: " + e.toString());
            }
        }

        return lookupTable;
    }
}
