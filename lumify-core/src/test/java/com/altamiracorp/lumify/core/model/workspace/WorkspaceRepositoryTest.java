package com.altamiracorp.lumify.core.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.InMemoryAuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.workspace.diff.WorkspaceDiff;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.id.QueueIdGenerator;
import com.altamiracorp.securegraph.inmemory.InMemoryAuthorizations;
import com.altamiracorp.securegraph.inmemory.InMemoryEdge;
import com.altamiracorp.securegraph.inmemory.InMemoryGraph;
import com.altamiracorp.securegraph.inmemory.InMemoryGraphConfiguration;
import com.altamiracorp.securegraph.search.DefaultSearchIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.altamiracorp.lumify.core.util.CollectionUtil.toList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WorkspaceRepositoryTest {
    private InMemoryGraph graph;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept rootConcept;

    @Mock
    private Concept workspaceConcept;

    @Mock
    private Relationship workspaceToEntityRelationship;

    @Mock
    private Relationship workspaceToUserRelationship;

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkspaceDiff workspaceDiff;

    @Mock
    private User user1;
    private Vertex user1Vertex;

    @Mock
    private User user2;
    private Vertex user2Vertex;

    private QueueIdGenerator idGenerator;

    private WorkspaceRepository workspaceRepository;
    private AuthorizationRepository authorizationRepository;
    private Vertex entity1Vertex;
    private Vertex entity2Vertex;
    private LumifyVisibility lumifyVisibility;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        lumifyVisibility = new LumifyVisibility(WorkspaceRepository.VISIBILITY_STRING);
        idGenerator = new QueueIdGenerator();
        graph = new InMemoryGraph(config, idGenerator, new DefaultSearchIndex(config.getConfig()));
        authorizationRepository = new InMemoryAuthorizationRepository();

        when(ontologyRepository.getConceptById(eq(OntologyRepository.ROOT_CONCEPT_IRI))).thenReturn(rootConcept);

        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq(WorkspaceRepository.WORKSPACE_CONCEPT_NAME), anyString())).thenReturn(workspaceConcept);
        when(workspaceConcept.getId()).thenReturn(WorkspaceRepository.WORKSPACE_CONCEPT_NAME);

        when(workspaceToEntityRelationship.getId()).thenReturn("workspaceToEntityRelationshipId");
        when(ontologyRepository.getOrCreateRelationshipType(eq(workspaceConcept), eq(rootConcept), eq(WorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_NAME), anyString())).thenReturn(workspaceToEntityRelationship);

        when(workspaceToUserRelationship.getId()).thenReturn("workspaceToUserRelationshipId");
        when(ontologyRepository.getOrCreateRelationshipType(eq(workspaceConcept), eq(rootConcept), eq(WorkspaceRepository.WORKSPACE_TO_USER_RELATIONSHIP_NAME), anyString())).thenReturn(workspaceToUserRelationship);

        workspaceRepository = new WorkspaceRepository(graph, ontologyRepository, userRepository, authorizationRepository, workspaceDiff);

        String user1Id = "USER_testUser1";
        when(user1.getUserId()).thenReturn(user1Id);

        user1Vertex = graph.addVertex(user1Id, lumifyVisibility.getVisibility(), new InMemoryAuthorizations(UserRepository.VISIBILITY_STRING));
        when(userRepository.findById(eq(user1Id))).thenReturn(user1Vertex);

        String user2Id = "USER_testUser2";
        when(user2.getUserId()).thenReturn(user2Id);
        user2Vertex = graph.addVertex(user2Id, lumifyVisibility.getVisibility(), new InMemoryAuthorizations(UserRepository.VISIBILITY_STRING));
        when(userRepository.findById(eq(user2Id))).thenReturn(user2Vertex);

        Object entity1VertexId = "entity1Id";
        entity1Vertex = graph.addVertex(entity1VertexId, new LumifyVisibility().getVisibility(), new InMemoryAuthorizations());
        Object entity2VertexId = "entity2Id";
        entity2Vertex = graph.addVertex(entity2VertexId, new LumifyVisibility().getVisibility(), new InMemoryAuthorizations());
    }

    @Test
    public void testAddWorkspace() {
        int startingVertexCount = graph.getAllVertices().size();
        int startingEdgeCount = graph.getAllEdges().size();

        String workspaceId = "testWorkspaceId";
        idGenerator.push(workspaceId);
        idGenerator.push(workspaceId + "_to_" + user1.getUserId());

        Workspace workspace = workspaceRepository.add("workspace1", user1);
        assertTrue(authorizationRepository.getGraphAuthorizations().contains(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId));

        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 1, graph.getAllEdges().size()); // +1 = the edge between workspace and user1

        assertNull("Should not have access", graph.getVertex(workspace.getId(), new InMemoryAuthorizations()));
        InMemoryAuthorizations authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getId());
        assertNotNull("Should have access", graph.getVertex(workspace.getId(), authorizations));

        when(userRepository.getAuthorizations(eq(user1), eq(WorkspaceRepository.VISIBILITY_STRING), eq(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId))).thenReturn(authorizations);
        Workspace foundWorkspace = workspaceRepository.findById(workspace.getId(), user1);
        assertEquals(workspace.getId(), foundWorkspace.getId());

        assertEquals(user1.getUserId(), foundWorkspace.getCreatorUserId());
    }

    @Test
    public void testAccessControl() {
        int startingVertexCount = graph.getAllVertices().size();
        int startingEdgeCount = graph.getAllEdges().size();

        String workspace1Id = "testWorkspace1Id";
        String workspace1Title = "workspace1";
        idGenerator.push(workspace1Id);
        idGenerator.push(workspace1Id + "_to_" + user1.getUserId());
        Workspace workspace1 = workspaceRepository.add(workspace1Title, user1);

        String workspace2Id = "testWorkspace2Id";
        String workspace2Title = "workspace2";
        idGenerator.push(workspace2Id);
        idGenerator.push(workspace2Id + "_to_" + user1.getUserId());
        Workspace workspace2 = workspaceRepository.add(workspace2Title, user1);

        String workspace3Id = "testWorkspace3Id";
        String workspace3Title = "workspace3";
        idGenerator.push(workspace3Id);
        idGenerator.push(workspace3Id + "_to_" + user2.getUserId());
        Workspace workspace3 = workspaceRepository.add(workspace3Title, user2);

        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 3, graph.getAllEdges().size()); // +3 = the edges between workspaces and users

        when(userRepository.getAuthorizations(eq(user1), eq(WorkspaceRepository.VISIBILITY_STRING), eq(UserRepository.VISIBILITY_STRING))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(userRepository.getAuthorizations(eq(user1), eq(WorkspaceRepository.VISIBILITY_STRING), eq(workspace3.getId()))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(user1.getUserId()).thenReturn("USER_testUser1");

        when(userRepository.getAuthorizations(eq(user2), eq(WorkspaceRepository.VISIBILITY_STRING), eq(UserRepository.VISIBILITY_STRING))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(userRepository.getAuthorizations(eq(user2), eq(WorkspaceRepository.VISIBILITY_STRING), eq(workspace3.getId()))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(userRepository.getAuthorizations(eq(user2), eq(UserRepository.VISIBILITY_STRING), eq(WorkspaceRepository.VISIBILITY_STRING), eq(workspace3.getId()))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(userRepository.getAuthorizations(eq(user2), eq(UserRepository.VISIBILITY_STRING), eq(LumifyVisibility.VISIBILITY_STRING), eq(workspace3.getId()))).thenReturn(new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, UserRepository.VISIBILITY_STRING));
        when(user2.getUserId()).thenReturn("USER_testUser2");

        List<Workspace> user1Workspaces = toList(workspaceRepository.findAll(user1));
        assertEquals(2, user1Workspaces.size());
        boolean foundWorkspace1 = false;
        boolean foundWorkspace2 = false;
        for (Workspace workspace : user1Workspaces) {
            if (workspace.getTitle().equals(workspace1Title)) {
                foundWorkspace1 = true;
            } else if (workspace.getTitle().equals(workspace2Title)) {
                foundWorkspace2 = true;
            }
        }
        assertTrue("foundWorkspace1", foundWorkspace1);
        assertTrue("foundWorkspace2", foundWorkspace2);

        List<Workspace> user2Workspaces = toList(workspaceRepository.findAll(user2));
        assertEquals(1, user2Workspaces.size());
        assertEquals(workspace3Title, user2Workspaces.get(0).getTitle());

        try {
            workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user1);
            fail("user1 should not have access to user2's workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getId(), ex.getResourceId());
        }

        idGenerator.push(workspace3Id + "to" + user2.getUserId());
        workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user2);
        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 4, graph.getAllEdges().size()); // +4 = the edges between workspaces and users
        List<WorkspaceUser> usersWithAccess = workspaceRepository.findUsersWithAccess(user2Workspaces.get(0), user2);
        boolean foundUser1 = false;
        boolean foundUser2 = false;
        for (WorkspaceUser userWithAccess : usersWithAccess) {
            if (userWithAccess.getUserId().equals(user1.getUserId())) {
                assertEquals(WorkspaceAccess.READ, userWithAccess.getWorkspaceAccess());
                foundUser1 = true;
            } else if (userWithAccess.getUserId().equals(user2.getUserId())) {
                assertEquals(WorkspaceAccess.WRITE, userWithAccess.getWorkspaceAccess());
                foundUser2 = true;
            } else {
                fail("Unexpected user " + userWithAccess.getUserId());
            }
        }
        assertTrue("could not find user1", foundUser1);
        assertTrue("could not find user2", foundUser2);

        try {
            workspaceRepository.deleteUserFromWorkspace(user2Workspaces.get(0), user1.getUserId(), user1);
            fail("user1 should not have write access to user2's workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getId(), ex.getResourceId());
        }

        try {
            workspaceRepository.delete(user2Workspaces.get(0), user1);
            fail("user1 should not have write access to user2's workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getId(), ex.getResourceId());
        }

        workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.WRITE, user2);
        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 4, graph.getAllEdges().size()); // +4 = the edges between workspaces and users

        workspaceRepository.deleteUserFromWorkspace(user2Workspaces.get(0), user1.getUserId(), user2);
        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 3, graph.getAllEdges().size()); // +3 = the edges between workspaces and users

        workspaceRepository.delete(user2Workspaces.get(0), user2);
        assertEquals(startingVertexCount + 2, graph.getAllVertices().size()); // +2 = the workspace vertices
        assertEquals(startingEdgeCount + 2, graph.getAllEdges().size()); // +2 = the edges between workspaces and users
    }

    @Test
    public void testEntities() {
        int startingVertexCount = graph.getAllVertices().size();
        int startingEdgeCount = graph.getAllEdges().size();

        String workspaceId = "testWorkspaceId";
        idGenerator.push(workspaceId);
        idGenerator.push(workspaceId + "_to_" + user1.getUserId());

        Workspace workspace = workspaceRepository.add("workspace1", user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 1, graph.getAllEdges().size()); // +1 = the edges between workspaces and users

        InMemoryAuthorizations user1Authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getId());
        when(userRepository.getAuthorizations(eq(user1), eq(WorkspaceRepository.VISIBILITY_STRING), eq(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId))).thenReturn(user1Authorizations);
        InMemoryAuthorizations user2Authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING);
        when(userRepository.getAuthorizations(eq(user2), eq(WorkspaceRepository.VISIBILITY_STRING), eq(WorkspaceRepository.WORKSPACE_ID_PREFIX + workspaceId))).thenReturn(user2Authorizations);

        try {
            workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, 100, 100, user2);
            fail("user2 should not have write access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getId(), ex.getResourceId());
        }

        idGenerator.push(workspaceId + "_to_" + entity1Vertex.getId());
        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, 100, 200, user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, graph.getAllEdges().size()); // +2 = the edges between workspaces, users, and entities

        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, 200, 300, user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, graph.getAllEdges().size()); // +2 = the edges between workspaces, users, and entities

        List<WorkspaceEntity> entities = workspaceRepository.findEntities(workspace, user1);
        assertEquals(1, entities.size());
        assertEquals(entity1Vertex.getId(), entities.get(0).getEntityVertexId());
        assertEquals(200, entities.get(0).getGraphPositionX());
        assertEquals(300, entities.get(0).getGraphPositionY());

        try {
            workspaceRepository.findEntities(workspace, user2);
            fail("user2 should not have read access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getId(), ex.getResourceId());
        }

        try {
            workspaceRepository.deleteEntityFromWorkspace(workspace, entity1Vertex.getId(), user2);
            fail("user2 should not have write access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getId(), ex.getResourceId());
        }

        workspaceRepository.deleteEntityFromWorkspace(workspace, entity1Vertex.getId(), user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        Map<Object, InMemoryEdge> edgesAfterDelete = graph.getAllEdges();
        assertEquals(startingEdgeCount + 2, edgesAfterDelete.size()); // +1 = the edges between workspaces, users
        boolean foundRemovedEdge = false;
        for (InMemoryEdge edge : edgesAfterDelete.values()) {
            if (edge.getLabel().equals(workspaceToEntityRelationship.getId())) {
                assertEquals(false, WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.getPropertyValue(edge));
                foundRemovedEdge = true;
            }
        }
        assertTrue(foundRemovedEdge);
    }
}
