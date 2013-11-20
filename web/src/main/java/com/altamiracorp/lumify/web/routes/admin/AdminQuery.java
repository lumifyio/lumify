package com.altamiracorp.lumify.web.routes.admin;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class AdminQuery extends BaseRequestHandler {

    private final ModelSession modelSession;

    @Inject
    public AdminQuery(ModelSession modelSession) {
        this.modelSession = modelSession;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        final String tableName = getRequiredParameter(request, "tableName");
        final String beginKey = decodeKey(getRequiredParameter(request, "beginKey"));
        final String endEnd = decodeKey(getRequiredParameter(request, "endEnd"));

        User user = getUser(request);

        List<Row> rows = modelSession.findByRowKeyRange(tableName, beginKey, endEnd, user.getModelUserContext());

        JSONObject results = new JSONObject();
        JSONArray rowsJson = new JSONArray();
        for (Row row : rows) {
            rowsJson.put(row.toJson());
        }
        results.put("rows", rowsJson);

        respondWithJson(response, results);
    }

    private String decodeKey(String key) {
        key = key.replaceAll("\\\\x", "\\\\u00");
        return StringEscapeUtils.unescapeJava(key);
    }
}
