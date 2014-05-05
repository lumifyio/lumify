package io.lumify.web.routes.user;

import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.core.user.User;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class UserList extends BaseRequestHandler {
    @Inject
    public UserList(
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String query = getOptionalParameter(request, "q");

        Iterable<User> users = getUserRepository().find(query);

        JSONObject resultJson = new JSONObject();
        JSONArray usersJson = getJson(users);
        resultJson.put("users", usersJson);

        respondWithJson(response, resultJson);
    }

    private JSONArray getJson(Iterable<User> users) throws JSONException {
        JSONArray usersJson = new JSONArray();
        for (User user : users) {
            usersJson.put(getUserRepository().toJson(user));
        }
        return usersJson;
    }
}
