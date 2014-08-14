package io.lumify.themoviedb.download;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class TheMovieDb {
    private static final String BASE_URL = "https://api.themoviedb.org/3/";
    private final String apiKey;
    private JSONObject configurationJson;

    public TheMovieDb(String apiKey) {
        this.apiKey = apiKey;
    }

    public JSONObject getPersonInfo(int personId) {
        String str = getUrl(BASE_URL + "person/" + personId + "?append_to_response=combined_credits&api_key=" + apiKey);
        return new JSONObject(str);
    }

    public JSONObject getMovieInfo(int movieId) {
        String str = getUrl(BASE_URL + "movie/" + movieId + "?append_to_response=credits&api_key=" + apiKey);
        return new JSONObject(str);
    }

    private String getUrl(String urlToRead) {
        return new String(getUrlBytes(urlToRead));
    }

    private byte[] getUrlBytes(String urlToRead) {
        try {
            URL url = new URL(urlToRead);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            InputStream in = conn.getInputStream();
            try {
                return IOUtils.toByteArray(in);
            } finally {
                in.close();
            }
        } catch (IOException ex) {
            throw new RuntimeException("Could not download: " + urlToRead, ex);
        }
    }

    public byte[] getImage(String profileImage) {
        String imageBaseUrl = getImageBaseUrl();
        return getUrlBytes(imageBaseUrl + "original" + profileImage);
    }

    private String getImageBaseUrl() {
        JSONObject configurationJson = getConfigurationJson();
        JSONObject images = configurationJson.getJSONObject("images");
        return images.getString("base_url");
    }

    private JSONObject getConfigurationJson() {
        if (configurationJson == null) {
            String configuration = getUrl(BASE_URL + "configuration?api_key=" + apiKey);
            configurationJson = new JSONObject(configuration);
        }
        return configurationJson;
    }

    public JSONObject getProductionCompanyInfo(int productionCompanyId) {
        String str = getUrl(BASE_URL + "company/" + productionCompanyId + "?api_key=" + apiKey);
        return new JSONObject(str);
    }
}
