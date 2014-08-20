package io.lumify.geocoder.bing;

import io.lumify.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class BingGeocoder extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BingGeocoder.class);
    private static final String CONFIG_KEY = "geocoder.bing.key";
    private String key;

    @Inject
    public BingGeocoder(
            UserRepository userRepository,
            WorkspaceRepository workspaceRepository,
            Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);

        key = configuration.get(CONFIG_KEY, null);
        if (key == null) {
            LOGGER.error("Could not find bing geocoder configuration key: " + CONFIG_KEY);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String query = getRequiredParameter(request, "q");
        JSONObject resultJson = queryBing(query);
        respondWithJson(response, resultJson);
    }

    private JSONObject queryBing(String query) throws IOException {
        String urlString = "http://dev.virtualearth.net/REST/v1/Locations?query=" + URLEncoder.encode(query, "utf-8") + "&output=json&key=" + URLEncoder.encode(key, "utf-8");
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        String responseText = IOUtils.toString(connection.getInputStream(), "UTF-8");
        JSONObject responseJson = new JSONObject(responseText);
        return toLumifyGeocodeJson(responseJson);
    }

    private JSONObject toLumifyGeocodeJson(JSONObject responseJson) {
        JSONArray results = new JSONArray();

        JSONArray resourceSets = responseJson.getJSONArray("resourceSets");
        for (int resourceSetIndex = 0; resourceSetIndex < resourceSets.length(); resourceSetIndex++) {
            JSONObject resourceSet = resourceSets.getJSONObject(resourceSetIndex);
            JSONArray resources = resourceSet.getJSONArray("resources");
            for (int resourceIndex = 0; resourceIndex < resources.length(); resourceIndex++) {
                JSONObject resource = resources.getJSONObject(resourceIndex);
                results.put(bingResourceToLumifyResult(resource));
            }
        }

        JSONObject json = new JSONObject();
        json.put("results", results);
        return json;
    }

    private JSONObject bingResourceToLumifyResult(JSONObject resource) {
        JSONArray coordinates = resource.getJSONObject("point").getJSONArray("coordinates");

        JSONObject json = new JSONObject();
        json.put("name", resource.getString("name"));
        json.put("latitude", coordinates.getDouble(0));
        json.put("longitude", coordinates.getDouble(1));
        return json;
    }

    public static boolean verifyConfiguration(Configuration configuration) {
        String key = configuration.get(CONFIG_KEY, null);
        if (key == null) {
            LOGGER.error("Could not find bing geocoder configuration key '" + CONFIG_KEY + "'. Geocoding will not be enabled.");
            return false;
        }
        return true;
    }
}
