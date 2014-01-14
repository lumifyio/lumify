package com.altamiracorp.lumify.web.routes.audit;

import com.altamiracorp.lumify.core.model.audit.Audit;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class VertexAudit extends BaseRequestHandler {

    private final AuditRepository auditRepository;

    @Inject
    public VertexAudit(final AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getAttributeString(request, "graphVertexId");
        Iterable<Audit> rows = auditRepository.findByRowStartsWith(graphVertexId, getUser(request).getModelUserContext());

        JSONObject results = new JSONObject();
        JSONArray audits = new JSONArray();
        for (Audit audit : rows) {
            JSONObject data = audit.toJson();
            audits.put(data);
        }
        results.put("auditHistory", audits);
        respondWithJson(response, results);
    }
}
