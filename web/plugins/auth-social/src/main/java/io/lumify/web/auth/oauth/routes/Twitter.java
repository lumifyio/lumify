package io.lumify.web.auth.oauth.routes;

import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.CurrentUser;
import io.lumify.web.auth.oauth.OAuthConfiguration;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;

public class Twitter implements Handler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Twitter.class);
    private static final String OAUTH_REQUEST_TOKEN = "oauth_token";
    public static final String OAUTH_TOKEN_PARAM_NAME = "oauth_token";
    public static final String OAUTH_VERIFIER_PARAM_NAME = "oauth_verifier";
    private final OAuthConfiguration config;
    private final UserRepository userRepository;

    public Twitter(OAuthConfiguration config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        checkNotNull(config.getKey(), "Twitter OAuth apiKey not set");
        checkNotNull(config.getSecret(), "Twitter OAuth apiSecret not set");
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
        OAuthService service = getOAuthService(httpRequest, true);
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
        OAuthService service = getOAuthService(httpRequest, false);

        // TODO: Store this token if authorization succeeds
        Token accessToken = service.getAccessToken(storedToken, verifier);

        OAuthRequest authRequest = new OAuthRequest(Verb.GET, "https://api.twitter.com/1.1/account/verify_credentials.json");
        service.signRequest(accessToken, authRequest);
        Response authResponse = authRequest.send();

        if (!authResponse.isSuccessful()) {
            LOGGER.warn("OAuth handshake completed, but Twitter credential verification failed: " + authResponse.getMessage());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

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

        String username = "twitter/" + screenName;
        User user = userRepository.findByUsername(username);
        if (user == null) {
            // For form based authentication, username and displayName will be the same
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.addUser(username, displayName, null, randomPassword, new String[0]);
        }
        userRepository.recordLogin(user, httpRequest.getRemoteAddr());

        CurrentUser.set(httpRequest, user.getUserId(), user.getUsername());

        httpResponse.sendRedirect(httpRequest.getServletContext().getContextPath() + "/");
    }

    private OAuthService getOAuthService(HttpServletRequest request, boolean withCallback) {
        ServiceBuilder builder = new ServiceBuilder()
                .provider(TwitterApi.SSL.Authenticate.class)
                .apiKey(config.getKey())
                .apiSecret(config.getSecret());

        if (withCallback) {
            builder.callback(request.getRequestURL().toString());
        }

        return builder.build();
    }
}
