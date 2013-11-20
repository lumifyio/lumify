package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class AdminTables extends BaseRequestHandler {
    private final ModelSession modelSession;

    @Inject
    public AdminTables(ModelSession modelSession) {
        this.modelSession = modelSession;
    }


    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        User user = getUser(request);
        List<String> tables = this.modelSession.getTableList(user.getModelUserContext());

        JSONObject results = new JSONObject();
        JSONArray tablesJson = new JSONArray();
        for (String table : tables) {
            tablesJson.put(table);
        }
        results.put("tables", tablesJson);

        respondWithJson(response, results);
    }
}
