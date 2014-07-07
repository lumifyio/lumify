package io.lumify.flightTrack;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

public class FlightAwareClient {
    public static final String FLIGHT_AWARE_JSON_URL = "http://flightxml.flightaware.com/json/FlightXML2/Search";
    private final String apiKey;
    private final String userName;

    public FlightAwareClient(String apiKey, String userName) {
        this.apiKey = apiKey;
        this.userName = userName;
    }

    public JSONObject search(String query) throws IOException {
        String urlEncodedQuery = URLEncoder.encode(query, "UTF-8");
        URL url = new URL(FLIGHT_AWARE_JSON_URL + "?howMany=100&query=" + urlEncodedQuery);
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("Authorization", "Basic " + getAuthString());
        InputStream in = uc.getInputStream();
        try {
            String jsonString = IOUtils.toString(in);
            return new JSONObject(jsonString);
        } finally {
            in.close();
        }
    }

    public String getAuthString() {
        String authString = userName + ":" + apiKey;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        return new String(authEncBytes);
    }
}
