package io.lumify.web.auth.oauth.routes;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.web.CurrentUser;
import io.lumify.web.auth.oauth.OAuthConfiguration;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.GoogleApi;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class Google implements Handler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Google.class);
    private static final String PASSWORD = "8XXuk2tQ523b";
    private static final String OAUTH_REQUEST_TOKEN = "oauth_token";
    public static final String OAUTH_TOKEN_PARAM_NAME = "oauth_token";
    public static final String OAUTH_VERIFIER_PARAM_NAME = "oauth_verifier";
    private final OAuthConfiguration config;
    private final UserRepository userRepository;

    public Google(OAuthConfiguration config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        checkNotNull(config.getKey(), "OAuth id not set");
        checkNotNull(config.getSecret(), "OAuth secret not set");
    }

    @Override
    public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws Exception {
        if (httpRequest.getParameter(OAUTH_VERIFIER_PARAM_NAME) != null) {
            verify(httpRequest, httpResponse, chain);
        } else {
            login(httpRequest, httpResponse, chain);
        }
    }

    private void login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws IOException {
        OAuthService service = getOAuthService(httpRequest, true, null);
        Token requestToken = service.getRequestToken();
        httpRequest.getSession().setAttribute(OAUTH_REQUEST_TOKEN, requestToken);
        String authUrl = service.getAuthorizationUrl(requestToken);
        httpResponse.sendRedirect(authUrl);
    }

    private void verify(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws IOException {
        String inboundVerifier = httpRequest.getParameter(OAUTH_VERIFIER_PARAM_NAME);
        String inboundToken = httpRequest.getParameter(OAUTH_TOKEN_PARAM_NAME);
        Token storedToken = (Token) httpRequest.getSession().getAttribute(OAUTH_REQUEST_TOKEN);
        httpRequest.getSession().removeAttribute(OAUTH_REQUEST_TOKEN);

        if (storedToken == null) {
            LOGGER.warn("OAuth verification attempted but no stored token found in the user's session");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!inboundToken.equals(storedToken.getToken())) {
            LOGGER.warn("OAuth verfication attempted, but oauth_token request param did not match token stored in user's session");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        Verifier verifier = new Verifier(inboundVerifier);
        OAuthService service = getOAuthService(httpRequest, false, "https://www.googleapis.com/auth/plus.login");

        // TODO: Store this token if authorization succeeds
        Token accessToken = service.getAccessToken(storedToken, verifier);

        OAuthRequest authRequest = new OAuthRequest(Verb.GET, "https://www.googleapis.com/plus/v1/people/me");
        service.signRequest(accessToken, authRequest);
        Response authResponse = authRequest.send();

        if (!authResponse.isSuccessful()) {
            LOGGER.warn("OAuth handshake completed, but credential verification failed: " + authResponse.getMessage());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        System.out.println(authResponse.getBody());
        httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);

        JSONObject jsonResponse = new JSONObject(authResponse.getBody());
        String screenName = jsonResponse.getString("screen_name");
        if (screenName == null) {
            LOGGER.warn("Twitter OAuth JSON authorization response did not contain a 'screen_name' value, which is required.");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String displayName = jsonResponse.getString("name");
        if (displayName == null) {
            LOGGER.warn("Twitter OAuth JSON authorization response did not contain a 'name' value, which is required.");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String username = "google/" + screenName;
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            user = userRepository.addUser(username, displayName, PASSWORD, new String[0]);
        }

        CurrentUser.set(httpRequest, user);

        httpResponse.sendRedirect(httpRequest.getServletContext().getContextPath() + "/");
    }

    private OAuthService getOAuthService(HttpServletRequest request, boolean withCallback, String scope) {
        ServiceBuilder builder = new ServiceBuilder()
                .provider(GoogleApi.class)
                .apiKey(config.getKey())
                .apiSecret(config.getSecret());

        if (scope != null) {
            builder.scope(scope);
        }

        if (withCallback) {
            builder.callback(request.getRequestURL().toString());
        }

        return builder.build();
    }
}
