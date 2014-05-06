package io.lumify.web.auth.oauth;

import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.web.AuthenticationProvider;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class OAuthAuthenticationProvider extends AuthenticationProvider {
    @Override
    public boolean login(HttpServletRequest request) {
        return false;
    }

    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, HandlerChain handlerChain) throws Exception {

    }
}
