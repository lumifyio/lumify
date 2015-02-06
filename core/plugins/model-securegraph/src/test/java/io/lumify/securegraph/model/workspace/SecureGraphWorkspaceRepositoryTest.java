package io.lumify.securegraph.model.workspace;

import com.google.common.collect.Lists;
import io.lumify.core.config.Configuration;
import io.lumify.core.config.HashMapConfigurationLoader;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.model.lock.LocalLockRepository;
import io.lumify.core.model.lock.LockRepository;
import io.lumify.core.model.ontology.InMemoryOntologyRepository;
import io.lumify.core.model.user.*;
import io.lumify.core.model.workspace.*;
import io.lumify.core.model.workspace.diff.WorkspaceDiffHelper;
import io.lumify.core.security.LumifyVisibility;
import io.lumify.web.clientapi.model.GraphPosition;
import io.lumify.web.clientapi.model.WorkspaceAccess;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.securegraph.Authorizations;
import org.securegraph.Vertex;
import org.securegraph.Visibility;
import org.securegraph.id.QueueIdGenerator;
import org.securegraph.inmemory.InMemoryAuthorizations;
import org.securegraph.inmemory.InMemoryEdge;
import org.securegraph.inmemory.InMemoryGraph;
import org.securegraph.inmemory.InMemoryGraphConfiguration;
import org.securegraph.search.DefaultSearchIndex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.*;
import static org.securegraph.util.IterableUtils.toList;

@RunWith(MockitoJUnitRunner.class)
public class SecureGraphWorkspaceRepositoryTest {
    private InMemoryGraph graph;

    @Mock
    private WorkspaceDiffHelper workspaceDiff;

    private InMemoryUser user1;

    private InMemoryUser user2;

    private QueueIdGenerator idGenerator;

    private WorkspaceRepository workspaceRepository;
    private AuthorizationRepository authorizationRepository;
    private Vertex entity1Vertex;

    @Mock
    private UserListenerUtil userListenerUtil;

    @Before
    public void setup() throws Exception {
        Visibility visibility = new Visibility("");
        Authorizations authorizations = new InMemoryAuthorizations();
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        idGenerator = new QueueIdGenerator();
        graph = InMemoryGraph.create(config, idGenerator, new DefaultSearchIndex(config));
        authorizationRepository = new InMemoryAuthorizationRepository();

        Configuration lumifyConfiguration = new HashMapConfigurationLoader(new HashMap()).createConfiguration();
        LockRepository lockRepository = new LocalLockRepository(lumifyConfiguration);
        InMemoryUserRepository userRepository = new InMemoryUserRepository(lumifyConfiguration, userListenerUtil);
        user1 = (InMemoryUser) userRepository.addUser("user2", "user2", null, "none", new String[0]);
        graph.addVertex(user1.getUserId(), visibility, authorizations);

        user2 = (InMemoryUser) userRepository.addUser("user2", "user2", null, "none", new String[0]);
        graph.addVertex(user2.getUserId(), visibility, authorizations);

        InMemoryOntologyRepository ontologyRepository = new InMemoryOntologyRepository(graph, lumifyConfiguration);

        workspaceRepository = new SecureGraphWorkspaceRepository(ontologyRepository, graph, userRepository, authorizationRepository, workspaceDiff, lockRepository);

        String entity1VertexId = "entity1Id";
        entity1Vertex = graph.addVertex(entity1VertexId, new LumifyVisibility().getVisibility(), new InMemoryAuthorizations());
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

        assertNull("Should not have access", graph.getVertex(workspace.getWorkspaceId(), new InMemoryAuthorizations()));
        InMemoryAuthorizations authorizations = new InMemoryAuthorizations(WorkspaceRepository.VISIBILITY_STRING, workspace.getWorkspaceId());
        assertNotNull("Should have access", graph.getVertex(workspace.getWorkspaceId(), authorizations));

        Workspace foundWorkspace = workspaceRepository.findById(workspace.getWorkspaceId(), user1);
        assertEquals(workspace.getWorkspaceId(), foundWorkspace.getWorkspaceId());
    }

    @Test
    public void testAccessControl() {
        int startingVertexCount = graph.getAllVertices().size();
        int startingEdgeCount = graph.getAllEdges().size();

        String workspace1Id = "testWorkspace1Id";
        String workspace1Title = "workspace1";
        idGenerator.push(workspace1Id);
        idGenerator.push(workspace1Id + "_to_" + user1.getUserId());
        workspaceRepository.add(workspace1Title, user1);

        String workspace2Id = "testWorkspace2Id";
        String workspace2Title = "workspace2";
        idGenerator.push(workspace2Id);
        idGenerator.push(workspace2Id + "_to_" + user1.getUserId());
        workspaceRepository.add(workspace2Title, user1);

        String workspace3Id = "testWorkspace3Id";
        String workspace3Title = "workspace3";
        idGenerator.push(workspace3Id);
        idGenerator.push(workspace3Id + "_to_" + user2.getUserId());
        workspaceRepository.add(workspace3Title, user2);

        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 3, graph.getAllEdges().size()); // +3 = the edges between workspaces and users

        List<Workspace> user1Workspaces = toList(workspaceRepository.findAllForUser(user1));
        assertEquals(2, user1Workspaces.size());
        boolean foundWorkspace1 = false;
        boolean foundWorkspace2 = false;
        for (Workspace workspace : user1Workspaces) {
            if (workspace.getDisplayTitle().equals(workspace1Title)) {
                foundWorkspace1 = true;
            } else if (workspace.getDisplayTitle().equals(workspace2Title)) {
                foundWorkspace2 = true;
            }
        }
        assertTrue("foundWorkspace1", foundWorkspace1);
        assertTrue("foundWorkspace2", foundWorkspace2);

        List<Workspace> user2Workspaces = toList(workspaceRepository.findAllForUser(user2));
        assertEquals(1, user2Workspaces.size());
        assertEquals(workspace3Title, user2Workspaces.get(0).getDisplayTitle());

        try {
            workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user1);
            fail("user1 should not have access to user2's workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        idGenerator.push(workspace3Id + "to" + user2.getUserId());
        workspaceRepository.updateUserOnWorkspace(user2Workspaces.get(0), user1.getUserId(), WorkspaceAccess.READ, user2);
        assertEquals(startingVertexCount + 3, graph.getAllVertices().size()); // +3 = the workspace vertices
        assertEquals(startingEdgeCount + 4, graph.getAllEdges().size()); // +4 = the edges between workspaces and users
        List<WorkspaceUser> usersWithAccess = workspaceRepository.findUsersWithAccess(user2Workspaces.get(0).getWorkspaceId(), user2);
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
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
        }

        try {
            workspaceRepository.delete(user2Workspaces.get(0), user1);
            fail("user1 should not have write access to user2's workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user1, ex.getUser());
            assertEquals(user2Workspaces.get(0).getWorkspaceId(), ex.getResourceId());
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

        try {
            workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(100, 100), user2);
            fail("user2 should not have write access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        idGenerator.push(workspaceId + "_to_" + entity1Vertex.getId());
        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(100, 200), user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, graph.getAllEdges().size()); // +2 = the edges between workspaces, users, and entities

        workspaceRepository.updateEntityOnWorkspace(workspace, entity1Vertex.getId(), true, new GraphPosition(200, 300), user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        assertEquals(startingEdgeCount + 2, graph.getAllEdges().size()); // +2 = the edges between workspaces, users, and entities

        List<WorkspaceEntity> entities = workspaceRepository.findEntities(workspace, user1);
        assertEquals(1, entities.size());
        assertEquals(entity1Vertex.getId(), entities.get(0).getEntityVertexId());
        assertEquals(200, entities.get(0).getGraphPositionX().intValue());
        assertEquals(300, entities.get(0).getGraphPositionY().intValue());

        try {
            workspaceRepository.findEntities(workspace, user2);
            fail("user2 should not have read access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        try {
            workspaceRepository.softDeleteEntitiesFromWorkspace(workspace, Lists.newArrayList(entity1Vertex.getId()), user2);
            fail("user2 should not have write access to workspace");
        } catch (LumifyAccessDeniedException ex) {
            assertEquals(user2, ex.getUser());
            assertEquals(workspace.getWorkspaceId(), ex.getResourceId());
        }

        workspaceRepository.softDeleteEntitiesFromWorkspace(workspace, Lists.newArrayList(entity1Vertex.getId()), user1);
        assertEquals(startingVertexCount + 1, graph.getAllVertices().size()); // +1 = the workspace vertex
        Map<String, InMemoryEdge> edgesAfterDelete = graph.getAllEdges();
        assertEquals(startingEdgeCount + 2, edgesAfterDelete.size()); // +1 = the edges between workspaces, users
        boolean foundRemovedEdge = false;
        for (InMemoryEdge edge : edgesAfterDelete.values()) {
            if (edge.getLabel().equals(SecureGraphWorkspaceRepository.WORKSPACE_TO_ENTITY_RELATIONSHIP_IRI)) {
                assertEquals(false, WorkspaceLumifyProperties.WORKSPACE_TO_ENTITY_VISIBLE.getPropertyValue(edge));
                foundRemovedEdge = true;
            }
        }
        assertTrue(foundRemovedEdge);
    }
}
