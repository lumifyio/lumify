package io.lumify.core.model.audit;

import com.google.inject.Inject;
import io.lumify.core.model.ontology.Relationship;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.core.model.user.AuthorizationRepository;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.core.security.VisibilityTranslator;
import io.lumify.core.user.User;
import io.lumify.core.version.VersionService;
import org.json.JSONObject;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.mutation.ExistingElementMutation;

import java.util.Date;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

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
