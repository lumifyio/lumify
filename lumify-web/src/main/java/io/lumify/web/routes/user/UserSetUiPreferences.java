package io.lumify.web.routes.user;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserSetUiPreferences extends BaseRequestHandler {
    @Inject
    public UserSetUiPreferences(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        JSONObject uiPreferences = new JSONObject(getRequiredParameter(request, "ui-preferences"));

        User user = getUser(request);
        if (user == null || user.getUsername() == null) {
            respondWithNotFound(response);
            return;
        }

        getUserRepository().setUiPreferences(user, uiPreferences);

        user = getUser(request);
        JSONObject userJson = getUserRepository().toJsonWithAuths(user);
        respondWithJson(response, userJson);
    }
}
