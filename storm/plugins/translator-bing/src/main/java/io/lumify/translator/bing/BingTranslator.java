package io.lumify.translator.bing;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.translate.Translator;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

public class BingTranslator implements Translator {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(BingTranslator.class);
    public static final String CONFIG_CLIENT_ID = "translator.bing.clientId";
    public static final String CONFIG_CLIENT_SECRET = "translator.bing.clientSecret";
    private Configuration configuration;
    private Date tokenExpiresDate;
    private String accessToken;

    @Override
    public String translate(String text, String language, GraphPropertyWorkData data) {
        ensureUpToDateToken();

        try {
            LOGGER.debug("Translating text: %s %s", data.getElement().getId(), data.getProperty().toString());
            String urlString = "http://api.microsofttranslator.com/V2/Ajax.svc/Translate";
            urlString += "?to=en";
            urlString += "&text=" + URLEncoder.encode(text.substring(0, Math.min(text.length(), 10000)), "utf-8");
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            String translatedText = IOUtils.toString(connection.getInputStream(), "UTF-8");
            return translatedText;
        } catch (Exception ex) {
            throw new LumifyException("Could not translate text", ex);
        }
    }

    private void ensureUpToDateToken() {
        if (tokenExpiresDate != null && tokenExpiresDate.compareTo(new Date()) > 0) {
            return;
        }

        try {
            String clientId = this.configuration.get(CONFIG_CLIENT_ID, null);
            checkNotNull(clientId, "Configuration " + CONFIG_CLIENT_ID + " is required");
            String clientSecret = this.configuration.get(CONFIG_CLIENT_SECRET, null);
            checkNotNull(clientSecret, "Configuration " + CONFIG_CLIENT_SECRET + " is required");
            String scope = "http://api.microsofttranslator.com";
            String grantType = "client_credentials";

            String urlString = "https://datamarket.accesscontrol.windows.net/v2/OAuth2-13";

            String formData = "client_id=" + URLEncoder.encode(clientId, "utf-8");
            formData += "&client_secret=" + URLEncoder.encode(clientSecret, "utf-8");
            formData += "&scope=" + URLEncoder.encode(scope, "utf-8");
            formData += "&grant_type=" + URLEncoder.encode(grantType, "utf-8");

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(true);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("Content-Length", Long.toString(formData.getBytes().length));
            connection.getOutputStream().write(formData.getBytes());
            String results = IOUtils.toString(connection.getInputStream(), "UTF-8");
            JSONObject json = new JSONObject(results);
            accessToken = json.getString("access_token");
            int expiresIn = json.getInt("expires_in");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, expiresIn);
            tokenExpiresDate = calendar.getTime();
        } catch (Exception ex) {
            throw new LumifyException("Could not refresh the token", ex);
        }
    }

    @Inject
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }
}
