package io.lumify.core.model.audit;

import com.google.inject.Inject;
import io.lumify.core.model.user.AuthorizationRepository;

public class AuditRepository {
    public static final String AUDIT_VISIBILITY = "audit";

    @Inject
    public AuditRepository(AuthorizationRepository authorizationRepository) {
        authorizationRepository.addAuthorizationToGraph(AUDIT_VISIBILITY);
    }

}
