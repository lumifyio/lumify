package io.lumify.web.auth.oauth.routes;

import com.altamiracorp.miniweb.Handler;
import com.altamiracorp.miniweb.HandlerChain;
import io.lumify.web.MustacheTemplate;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

public class Login implements Handler {
    private static final String OAUTH_REQUEST_TOKEN = "oauth_token";
    private final MustacheTemplate template;

    public Login() {
        this.template = new MustacheTemplate("oauth/templates/login.mustache");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("context", request.getServletContext());
        this.template.render(data, response);
    }

}
