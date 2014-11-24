package io.lumify.web.auth.oauth.routes;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.http.HttpConnection;
import io.lumify.http.HttpGetMethod;
import io.lumify.http.HttpPostMethod;
import io.lumify.http.URLBuilder;
import io.lumify.miniweb.Handler;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.CurrentUser;
import io.lumify.web.auth.oauth.OAuthConfiguration;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.common.base.Preconditions.checkNotNull;

public class Google implements Handler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Google.class);
    private static final String OAUTH_STATE_ATTR_NAME = "oauth.state";
    private static final String DISCOVERY_DOC_URL = "https://accounts.google.com/.well-known/openid-configuration";

    private final OAuthConfiguration config;
    private final UserRepository userRepository;
    private String authorizationEndpoint;
    private String tokenEndpoint;
    private String userInfoEndpoint;

    public Google(OAuthConfiguration config, UserRepository userRepository) {
        this.config = config;
        this.userRepository = userRepository;
        checkNotNull(config.getKey(), "OAuth key not set");
        checkNotNull(config.getSecret(), "OAuth secret not set");
        discoverEndpoints();
    }

    @Override
    public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws Exception {
        if (httpRequest.getParameter("code") != null) {
            verify(httpRequest, httpResponse, chain);
        } else {
            login(httpRequest, httpResponse, chain);
        }
    }

    private void discoverEndpoints() {
        try {
            HttpGetMethod getMethod = new HttpGetMethod(new URL(DISCOVERY_DOC_URL));
            HttpConnection conn = getMethod.openConnection();

            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new LumifyException("Failed to retrieve OpenID Connect discovery document");
            }

            JSONObject json = new JSONObject(conn.getResponseAsString());
            this.authorizationEndpoint = json.getString("authorization_endpoint");
            checkNotNull(this.authorizationEndpoint);
            this.tokenEndpoint = json.getString("token_endpoint");
            checkNotNull(this.tokenEndpoint);
            this.userInfoEndpoint = json.getString("userinfo_endpoint");
            checkNotNull(this.userInfoEndpoint);
        } catch (Exception e) {
            throw new LumifyException("Error retrieving OpenID Connect discover document", e);
        }
    }

    private String generateState() {
        return UserRepository.createRandomPassword();
    }

    private void login(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws IOException {
        String state = generateState();
        httpRequest.getSession().setAttribute(OAUTH_STATE_ATTR_NAME, state);

        URLBuilder authUrl = new URLBuilder(this.authorizationEndpoint);
        authUrl.addParameter("state", state);
        authUrl.addParameter("client_id", this.config.getKey());
        authUrl.addParameter("response_type", "code");
        authUrl.addParameter("scope", "openid email profile");
        authUrl.addParameter("redirect_uri", httpRequest.getRequestURL().toString());

        httpResponse.sendRedirect(authUrl.build());
    }

    private void verify(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws IOException {
        validateState(httpRequest, httpResponse);
        if (httpResponse.isCommitted()) return;

        String code = getRequiredRequestParameter("code", httpRequest, httpResponse);
        if (httpResponse.isCommitted()) return;

        JSONObject accessTokenResponseJson = requestAccessToken(httpRequest, httpResponse, code);
        if (httpResponse.isCommitted()) return;

        String accessToken = getRequiredProperty(accessTokenResponseJson, "access_token", httpResponse);
        if (httpResponse.isCommitted()) return;

        String tokenType = getRequiredProperty(accessTokenResponseJson, "token_type", httpResponse);
        if (httpResponse.isCommitted()) return;

        JSONObject userInfo = getUserInfo(accessToken, tokenType, httpResponse);
        if (httpResponse.isCommitted()) return;

        String userid = getRequiredProperty(userInfo, "sub", httpResponse);
        if (httpResponse.isCommitted()) return;

        String displayName = getRequiredProperty(userInfo, "name", httpResponse);
        if (httpResponse.isCommitted()) return;

        String email = getRequiredProperty(userInfo, "email", httpResponse);
        if (httpResponse.isCommitted()) return;

        String username = "google/" + userid;
        User user = userRepository.findByUsername(username);
        if (user == null) {
            String randomPassword = UserRepository.createRandomPassword();
            user = userRepository.addUser(username, displayName, email, randomPassword, new String[0]);
        }
        userRepository.recordLogin(user, httpRequest.getRemoteAddr());

        CurrentUser.set(httpRequest, user.getUserId(), user.getUsername());
        httpResponse.sendRedirect(httpRequest.getServletContext().getContextPath() + "/");
    }

    private void validateState(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        Object storedState = httpRequest.getSession().getAttribute(OAUTH_STATE_ATTR_NAME);
        httpRequest.getSession().removeAttribute(OAUTH_STATE_ATTR_NAME);
        if (storedState == null) {
            LOGGER.warn("OAuth verification attempted but no stored state found in the user's session");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        String inboundState = httpRequest.getParameter("state");
        if (inboundState == null) {
            LOGGER.warn("OAuth verfication attempted, but no 'state' parameter found in the request");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (!storedState.equals(inboundState)) {
            LOGGER.warn("OAuth verfication attempted, but stored state does not match state param in the request");
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
    }

    private JSONObject requestAccessToken(HttpServletRequest httpRequest, HttpServletResponse httpResponse, String code) throws IOException {
        HttpPostMethod postMethod = new HttpPostMethod(new URL(this.tokenEndpoint));
        postMethod.addRequestParameter("code", code);
        postMethod.addRequestParameter("client_id", this.config.getKey());
        postMethod.addRequestParameter("client_secret", this.config.getSecret());
        postMethod.addRequestParameter("redirect_uri", httpRequest.getRequestURL().toString());
        postMethod.addRequestParameter("grant_type", "authorization_code");
        HttpConnection accessTokenConnection = postMethod.openConnection();

        if (accessTokenConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.error("Access token request failed: %s", accessTokenConnection.getResponseMessage());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        String accessTokenResponse = accessTokenConnection.getResponseAsString();
        return new JSONObject(accessTokenResponse);
    }

    private JSONObject getUserInfo(String accessToken, String tokenType, HttpServletResponse httpResponse) throws IOException {
        HttpGetMethod getMethod = new HttpGetMethod(new URL(this.userInfoEndpoint));
        getMethod.setHeader("Authorization", tokenType + " " + accessToken);
        HttpConnection userInfoConnection = getMethod.openConnection();

        if (userInfoConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            LOGGER.error("Request for user information failed: %s", userInfoConnection.getResponseMessage());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return new JSONObject(userInfoConnection.getResponseAsString());
    }

    private String getRequiredRequestParameter(String paramName, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException {
        String value = httpRequest.getParameter(paramName);
        if (value == null) {
            LOGGER.warn("Request parameter '%s' not found", paramName);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
        }
        return value;
    }

    private String getRequiredProperty(JSONObject json, String propName, HttpServletResponse httpResponse) throws IOException {
        String value = json.getString(propName);
        if (value == null) {
            LOGGER.error("No '%s' parameter found in response", propName);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
        return value;
    }

}
