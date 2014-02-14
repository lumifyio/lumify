package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.altamiracorp.lumify.core.model.ontology.OntologyLumifyProperties.CONCEPT_TYPE;

@Singleton
public class WorkspaceRepository {
    public static final String VISIBILITY_STRING = "workspace";
    public static final Visibility VISIBILITY = new Visibility(VISIBILITY_STRING);
    public static final String WORKSPACE_CONCEPT_NAME = "http://lumify.io/workspace";
    public static final String WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME = "http://lumify.io/workspace/toEntity";
    private final Graph graph;
    private final String workspaceConceptId;
    private final String workspaceToEntityRelationshipId;

    @Inject
    public WorkspaceRepository(final Graph graph, final UserProvider userProvider, final OntologyRepository ontologyRepository) {
        this.graph = graph;

        Concept entityConcept = ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY);

        Concept workspaceConcept = ontologyRepository.getOrCreateConcept(null, WORKSPACE_CONCEPT_NAME, "workspace");
        workspaceConceptId = workspaceConcept.getId();

        Relationship workspaceToEntityRelationship = ontologyRepository.getOrCreateRelationshipType(workspaceConcept, entityConcept, WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME, "workspace to entity");
        workspaceToEntityRelationshipId = workspaceToEntityRelationship.getId();
    }

    public void delete(Workspace workspace, User user) {
        graph.removeVertex(workspace.getVertex(), user.getAuthorizations(VISIBILITY_STRING));
    }

    public Workspace findById(String workspaceId, User user) {
        Vertex workspaceVertex = graph.getVertex(workspaceId, user.getAuthorizations(VISIBILITY_STRING));
        return new Workspace(workspaceVertex, user);
    }

    public Workspace add(String title, Vertex user) {
        WorkspaceRowKey workspaceRowKey = new WorkspaceRowKey(
                user.getId().toString(), String.valueOf(System.currentTimeMillis()));
        Workspace workspace = new Workspace(workspaceRowKey);
        workspace.setCreator(user.getId().toString());
    }

    public Iterable<Workspace> findAll(User user) {
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations(VISIBILITY_STRING))
                .has(CONCEPT_TYPE.getKey(), workspaceConceptId)
                .vertices();
        return Workspace.toWorkspaceIterable(vertices, user);
    }
}
