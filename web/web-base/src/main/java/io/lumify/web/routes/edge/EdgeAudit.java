package io.lumify.web.routes.edge;

import com.google.inject.Inject;
import io.lumify.core.config.Configuration;
import io.lumify.core.model.audit.Audit;
import io.lumify.core.model.audit.AuditEntity;
import io.lumify.core.model.audit.AuditRepository;
import io.lumify.core.model.user.UserRepository;
import io.lumify.core.model.workspace.WorkspaceRepository;
import io.lumify.web.clientapi.model.Privilege;
import io.lumify.core.user.User;
import io.lumify.miniweb.HandlerChain;
import io.lumify.web.BaseRequestHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class EdgeAudit extends BaseRequestHandler {
    private final AuditRepository auditRepository;

    @Inject
    public EdgeAudit(
            final AuditRepository auditRepository,
            final UserRepository userRepository,
            final WorkspaceRepository workspaceRepository,
            final Configuration configuration) {
        super(userRepository, workspaceRepository, configuration);
        this.auditRepository = auditRepository;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String edgeId = getAttributeString(request, "edgeId");
        User user = getUser(request);
        Iterable<Audit> rows = auditRepository.getAudits(edgeId, getActiveWorkspaceId(request), getAuthorizations(request, user));

        JSONObject results = new JSONObject();
        JSONArray audits = new JSONArray();
        for (Audit audit : rows) {
            JSONObject data = audit.toJson();
            if (data.keySet().contains(AuditEntity.ENTITY_AUDIT) && !getPrivileges(user).contains(Privilege.ADMIN)) {
                continue;
            }
            audits.put(data);
        }
        results.put("auditHistory", audits);
        respondWithJson(response, results);
    }
}
