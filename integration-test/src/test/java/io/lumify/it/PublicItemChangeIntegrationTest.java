package io.lumify.it;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.model.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class PublicItemChangeIntegrationTest extends TestBase {
    private ClientApiEdgeWithVertexData e1;
    private ClientApiElement v1;
    private ClientApiElement v2;

    @Test
    public void testPublicItemChanges() throws ApiException {
        createUsers();
        createTestGraph();
        testDeleteProperty();
        testDeleteEdge();
        testDeleteVertex();
    }

    private void createUsers() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");
        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_2, "auth1");
        lumifyApi.logout();
    }

    private void createTestGraph() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_1);
        addUserAuth(lumifyApi, USERNAME_TEST_USER_1, "auth1");

        v1 = lumifyApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "auth1");
        lumifyApi.getVertexApi().setProperty(v1.getId(), "key1", TestOntology.PROPERTY_NAME, "Joe", "auth1", "test");

        v2 = lumifyApi.getVertexApi().create(TestOntology.CONCEPT_PERSON, "auth1");

        e1 = lumifyApi.getEdgeApi().create(v1.getId(), v2.getId(), TestOntology.EDGE_LABEL_WORKS_FOR, "auth1");

        List<ClientApiWorkspaceDiff.Item> diffItems = lumifyApi.getWorkspaceApi().getDiff().getDiffs();
        lumifyApi.getWorkspaceApi().publishAll(diffItems);

        lumifyApi.logout();

        lumifyApi = login(USERNAME_TEST_USER_2);

        // add vertices to workspace
        ClientApiWorkspaceUpdateData updateData = new ClientApiWorkspaceUpdateData();
        updateData.getEntityUpdates().add(new ClientApiWorkspaceUpdateData.EntityUpdate(v1.getId(), new GraphPosition(0, 0)));
        updateData.getEntityUpdates().add(new ClientApiWorkspaceUpdateData.EntityUpdate(v2.getId(), new GraphPosition(0, 0)));
        lumifyApi.getWorkspaceApi().update(updateData);

        lumifyApi.logout();
    }

    private void testDeleteProperty() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);

        // delete the property
        lumifyApi.getVertexApi().deleteProperty(v1.getId(), "key1", TestOntology.PROPERTY_NAME);

        // verify the diff
        List<ClientApiWorkspaceDiff.Item> diffItems = lumifyApi.getWorkspaceApi().getDiff().getDiffs();
        assertEquals(1, diffItems.size());
        assertTrue("wrong diff type: " + diffItems.get(0).getClass().getName(), diffItems.get(0) instanceof ClientApiWorkspaceDiff.PropertyItem);
        ClientApiWorkspaceDiff.PropertyItem pi = (ClientApiWorkspaceDiff.PropertyItem) diffItems.get(0);
        assertEquals("key1", pi.getKey());
        assertEquals(TestOntology.PROPERTY_NAME, pi.getName());
        assertTrue("is deleted", pi.isDeleted());
        assertEquals("((auth1))|lumify", pi.getVisibilityString());

        // publish the delete
        ClientApiWorkspacePublishResponse publishResponse = lumifyApi.getWorkspaceApi().publishAll(diffItems);
        assertTrue("publish not success", publishResponse.isSuccess());
        assertEquals(0, publishResponse.getFailures().size());

        lumifyApi.logout();

        // verify all users see the delete
        lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiElement v1WithoutProperty = lumifyApi.getVertexApi().getByVertexId(v1.getId());
        assertEquals(2, v1WithoutProperty.getProperties().size());
        assertEquals(0, Collections2.filter(v1WithoutProperty.getProperties(), new Predicate<ClientApiProperty>() {
            @Override
            public boolean apply(ClientApiProperty prop) {
                return prop.getKey().equals("key1") && prop.getName().equals(TestOntology.PROPERTY_NAME);
            }
        }).size());

        lumifyApi.logout();
    }

    private void testDeleteEdge() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);

        // delete the edge
        lumifyApi.getVertexApi().deleteEdge(e1.getId());

        // verify the diff
        List<ClientApiWorkspaceDiff.Item> diffItems = lumifyApi.getWorkspaceApi().getDiff().getDiffs();
        assertEquals(1, diffItems.size());
        assertTrue("wrong diff type: " + diffItems.get(0).getClass().getName(), diffItems.get(0) instanceof ClientApiWorkspaceDiff.EdgeItem);
        ClientApiWorkspaceDiff.EdgeItem ei = (ClientApiWorkspaceDiff.EdgeItem) diffItems.get(0);
        assertEquals(e1.getId(), ei.getEdgeId());
        assertTrue("is deleted", ei.isDeleted());
        assertEquals(TestOntology.EDGE_LABEL_WORKS_FOR, ei.getLabel());

        // publish the delete
        ClientApiWorkspacePublishResponse publishResponse = lumifyApi.getWorkspaceApi().publishAll(diffItems);
        assertTrue("publish not success", publishResponse.isSuccess());
        assertEquals(0, publishResponse.getFailures().size());

        lumifyApi.logout();

        // verify all users see the delete
        lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiVertexEdges edges = lumifyApi.getVertexApi().getEdges(v1.getId());
        assertEquals(0, edges.getRelationships().size());

        lumifyApi.logout();
    }

    private void testDeleteVertex() throws ApiException {
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);

        // delete the vertex
        lumifyApi.getVertexApi().deleteVertex(v1.getId());

        // verify the diff
        List<ClientApiWorkspaceDiff.Item> diffItems = lumifyApi.getWorkspaceApi().getDiff().getDiffs();
        assertEquals(1, diffItems.size());
        assertTrue("wrong diff type: " + diffItems.get(0).getClass().getName(), diffItems.get(0) instanceof ClientApiWorkspaceDiff.VertexItem);
        ClientApiWorkspaceDiff.VertexItem vi = (ClientApiWorkspaceDiff.VertexItem) diffItems.get(0);
        assertEquals(v1.getId(), vi.getVertexId());
        assertTrue("is deleted", vi.isDeleted());

        // publish the delete
        ClientApiWorkspacePublishResponse publishResponse = lumifyApi.getWorkspaceApi().publishAll(diffItems);
        assertTrue("publish not success", publishResponse.isSuccess());
        assertEquals(0, publishResponse.getFailures().size());

        lumifyApi.logout();

        // verify all users see the delete
        lumifyApi = login(USERNAME_TEST_USER_1);

        ClientApiElement v = lumifyApi.getVertexApi().getByVertexId(v1.getId());
        assertNull("vertex should not have been found", v);

        lumifyApi.logout();
    }
}
