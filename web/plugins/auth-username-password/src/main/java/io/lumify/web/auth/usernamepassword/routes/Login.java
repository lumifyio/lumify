package io.lumify.web.auth.usernamepassword.routes;

import io.lumify.miniweb.HandlerChain;
import io.lumify.miniweb.utils.UrlUtils;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.AuthenticationHandler;
import io.lumify.web.BaseRequestHandler;
import io.lumify.web.CurrentUser;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Login extends BaseRequestHandler {

    @Inject
    public Login(UserRepository userRepository, WorkspaceRepository workspaceRepository, Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String username = UrlUtils.urlDecode(request.getParameter("username"));
        final String password = UrlUtils.urlDecode(request.getParameter("password")).trim();

        User user = getUserRepository().findByUsername(username);
        if (user != null && getUserRepository().isPasswordValid(user, password)) {
            getUserRepository().recordLogin(user, AuthenticationHandler.getRemoteAddr(request));
            CurrentUser.set(request, user.getUserId(), user.getUsername());
            JSONObject json = new JSONObject();
            json.put("status", "OK");
            respondWithJson(response, json);
        } else {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        }

    }
}
