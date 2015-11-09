package io.lumify.termsOfUse;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.apache.commons.codec.binary.Hex;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TermsOfUse extends BaseRequestHandler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(TermsOfUse.class);
    private JSONObject termsJson;
    private String termsHash;

    public static final String TITLE_PROPERTY = "termsOfUse.title";
    public static final String DEFAULT_TITLE = "Terms of Use";
    public static final String HTML_PROPERTY = "termsOfUse.html";
    public static final String DEFAULT_HTML = "<p>These are the terms to which you must agree before using Lumify:</p><p>This is a demonstration instance of Lumify. Features and data may change at any time.</p>" +
            "<p>Data entered into Lumify is not private and will not be maintained.</p>";
    public static final String DATE_PROPERTY = "termsOfUse.date";
    public static final String DATE_PROPERTY_FORMAT = "yyyy-MM-dd";

    private static final String UI_PREFERENCE_KEY = "termsOfUse";
    private static final String UI_PREFERENCE_HASH_SUBKEY = "hash";
    private static final String UI_PREFERENCE_DATE_SUBKEY = "date";

    @Inject
    protected TermsOfUse(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);

        String title = configuration.get(TITLE_PROPERTY, DEFAULT_TITLE);
        String html = configuration.get(HTML_PROPERTY, DEFAULT_HTML);
        termsHash = hash(html);
        Date date = null;
        String dateString = configuration.get(DATE_PROPERTY, null);
        if (dateString != null) {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PROPERTY_FORMAT);
            try {
                date = sdf.parse(dateString);
            } catch (ParseException e) {
                throw new LumifyException("unable to parse " + DATE_PROPERTY + " property with format " + DATE_PROPERTY_FORMAT, e);
            }
        }

        termsJson = new JSONObject();
        termsJson.put("title", title);
        termsJson.put("html", html);
        termsJson.put("hash", termsHash);
        if (date != null) {
            termsJson.put("date", date);
        }
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);

        if (request.getMethod().equals("POST")) {
            recordAcceptance(user, getRequiredParameter(request, "hash"));
            JSONObject successJson = new JSONObject();
            successJson.put("success", true);
            successJson.put("message", "Terms of Use accepted.");
            respondWithJson(response, successJson);
        } else {
            JSONObject termsAndStatus = new JSONObject();
            termsAndStatus.put("terms", termsJson);
            termsAndStatus.put("status", getStatus(user));
            respondWithJson(response, termsAndStatus);
        }
    }

    private String hash(String s) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(s.getBytes());
            return Hex.encodeHexString(md5);
        } catch (NoSuchAlgorithmException e) {
            throw new LumifyException("Could not find MD5", e);
        }
    }

    private JSONObject getUiPreferences(User user) {
        JSONObject uiPreferences = user.getUiPreferences();
        return uiPreferences != null ? uiPreferences : new JSONObject();
    }

    private void recordAcceptance(User user, String hash) {
        JSONObject uiPreferences = getUiPreferences(user);

        JSONObject touJson = new JSONObject();
        touJson.put(UI_PREFERENCE_HASH_SUBKEY, hash);
        touJson.put(UI_PREFERENCE_DATE_SUBKEY, new Date());
        uiPreferences.put(UI_PREFERENCE_KEY, touJson);
        getUserRepository().setUiPreferences(user, uiPreferences);
    }

    private JSONObject getStatus(User user) {
        JSONObject uiPreferences = getUiPreferences(user);
        JSONObject touJson = uiPreferences.optJSONObject(UI_PREFERENCE_KEY);

        JSONObject statusJson = new JSONObject();
        statusJson.put("current", false);

        if (touJson != null) {
            if (touJson.getString(UI_PREFERENCE_HASH_SUBKEY).equals(termsHash)) {
                statusJson.put("current", true);
                statusJson.put("accepted", touJson.getString(UI_PREFERENCE_DATE_SUBKEY));
            }
        }

        return statusJson;
    }
}
