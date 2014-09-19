package io.lumify.core.model.audit;

import com.google.inject.Inject;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.version.VersionService;
import org.securegraph.Graph;

public class AuditRepository {
    public static final String AUDIT_VISIBILITY = "audit";
    private final Graph graph;
    private final AuthorizationRepository authorizationRepository;
    private final VisibilityTranslator visibilityTranslator;
    private final VersionService versionService;

    @Inject
    public AuditRepository(Graph graph,
                           VisibilityTranslator visibilityTranslator,
                           AuthorizationRepository authorizationRepository,
                           VersionService versionService) {
        this.graph = graph;
        this.authorizationRepository = authorizationRepository;
        this.visibilityTranslator = visibilityTranslator;
        this.versionService = versionService;
        authorizationRepository.addAuthorizationToGraph(AUDIT_VISIBILITY);
    }

}
