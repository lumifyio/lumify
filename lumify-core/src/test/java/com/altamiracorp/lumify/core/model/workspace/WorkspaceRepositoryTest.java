package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.InMemoryAuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.id.UUIDIdGenerator;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceRepositoryTest {
    private Graph graph;

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

    private WorkspaceRepository workspaceRepository;
    private AuthorizationRepository authorizationRepository;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        graph = new InMemoryGraph(config, new UUIDIdGenerator(config.getConfig()), new DefaultSearchIndex(config.getConfig()));
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
        Workspace workspace = workspaceRepository.add("workspace1", user);

        assertNull("Should not have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations()));
        assertNull("Should not have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING)));
        assertNotNull("Should have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getId())));
    }
}
