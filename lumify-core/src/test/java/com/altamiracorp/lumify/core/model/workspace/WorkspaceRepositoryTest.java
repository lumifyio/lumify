package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.InMemoryAuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.id.IdGenerator;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryGraph;
import com.altamiracorp.securegraph.inmemory.InMemoryGraphConfiguration;
import com.altamiracorp.securegraph.search.DefaultSearchIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceRepositoryTest {
    private InMemoryGraph graph;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept entityConcept;

    @Mock
    private Concept workspaceConcept;

    @Mock
    private Relationship workspaceToEntityRelationship;

    @Mock
    private Relationship workspaceToUserRelationship;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    @Mock
    private Vertex userVertex;

    @Mock
    private IdGenerator idGenerator;

    private WorkspaceRepository workspaceRepository;
    private AuthorizationRepository authorizationRepository;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        graph = new InMemoryGraph(config, idGenerator, new DefaultSearchIndex(config.getConfig()));
        authorizationRepository = new InMemoryAuthorizationRepository();

        when(ontologyRepository.getConceptByName(eq(OntologyRepository.TYPE_ENTITY))).thenReturn(entityConcept);

        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq(WorkspaceRepository.WORKSPACE_CONCEPT_NAME), anyString())).thenReturn(workspaceConcept);
        when(workspaceConcept.getId()).thenReturn(WorkspaceRepository.WORKSPACE_CONCEPT_NAME);

        when(ontologyRepository.getOrCreateRelationshipType(eq(workspaceConcept), eq(entityConcept), eq(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME), anyString())).thenReturn(workspaceToEntityRelationship);

        when(ontologyRepository.getOrCreateRelationshipType(eq(workspaceConcept), eq(entityConcept), eq(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_NAME), anyString())).thenReturn(workspaceToUserRelationship);

        workspaceRepository = new WorkspaceRepository(graph, ontologyRepository, userRepository, authorizationRepository);

        String userId = "testUser";
        when(user.getUserId()).thenReturn(userId);
        when(userRepository.findById(eq(userId))).thenReturn(userVertex);
    }

    @Test
    public void testAddWorkspace() {
        int startingVertexCount = graph.getAllVertices().size();
        int startingEdgeCount = graph.getAllEdges().size();

        String workspaceId = "testWorkspaceId";
        when(idGenerator.nextId()).thenReturn(workspaceId);

        Workspace workspace = workspaceRepository.add("workspace1", user);
        verify(userRepository, times(1)).addAuthorization((Vertex) any(), eq(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId));

        assertNull("Should not have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations()));
        assertNull("Should not have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING)));
        InMemoryAuthorizations authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getId());
        assertNotNull("Should have access", graph.getVertex(workspace.getId(), authorizations));

        when(userRepository.getAuthorizations(eq(user), eq(WorkspaceRepository.VISIBILITY_STRING), eq(workspace.getId()))).thenReturn(authorizations);
        Workspace foundWorkspace = workspaceRepository.findById(workspace.getId(), user);
        assertEquals(workspace.getId(), foundWorkspace.getId());

        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 1, graph.getAllEdges().size()); // +1 = the edge between workspace and user
    }
}
