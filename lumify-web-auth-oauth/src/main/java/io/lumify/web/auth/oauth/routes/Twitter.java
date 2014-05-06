package io.lumify.web.auth.oauth.routes;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.*;
import org.scribe.oauth.OAuthService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Twitter implements Handler {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(Twitter.class);
    private static final String OAUTH_REQUEST_TOKEN = "oauth_token";
    public static final String OAUTH_TOKEN_PARAM_NAME = "oauth_token";
    public static final String OAUTH_VERIFIER_PARAM_NAME = "oauth_verifier";

    public Twitter() {

    }

    @Override
    public void handle(HttpServletRequest httpRequest, HttpServletResponse httpResponse, HandlerChain chain) throws Exception {
        if (httpRequest.getParameter(OAUTH_VERIFIER_PARAM_NAME) != null) {
            verify(httpRequest, httpResponse, chain);
        }

        login(httpRequest, httpResponse, chain);
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

        if (authResponse.getCode() != 200) {
            LOGGER.warn("OAuth handshake completed, but Twitter credential verification failed: " + authResponse.getMessage());
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        System.out.println(authResponse.getBody());

        httpResponse.sendRedirect("/oauth/success.html");
    }

    private OAuthService getOAuthService(HttpServletRequest request, boolean withCallback) {
        ServiceBuilder builder = new ServiceBuilder()
                .provider(TwitterApi.SSL.class)
                .apiKey("nzr1TkoPHupu7aSW9SjQ")
                .apiSecret("nC3os0GA14tNj0HtrcNIpf8p1CHcJlLHQCpJU5YI");

        if (withCallback) {
            builder.callback(request.getRequestURL().toString());
        }

        return builder.build();
    }
}
