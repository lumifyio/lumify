package io.lumify.core.model.audit;

import com.google.inject.Inject;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.version.VersionService;
import org.securegraph.Graph;

public class AuditRepository {
    public static final String AUDIT_VISIBILITY = "audit";

    @Inject
    public AuditRepository() {
        authorizationRepository.addAuthorizationToGraph(AUDIT_VISIBILITY);
    }

}
