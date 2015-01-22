package io.lumify.it;

import com.google.common.collect.ImmutableList;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.VertexApiExt;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.VertexApi;
import io.lumify.web.clientapi.model.ClientApiVertex;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class VertexIntegrationTest extends VertextTestBase {

    private static final String PROPERTY_QUERY_STRING = PROPERTY_VALUE_PREFIX;
    private static final String NO_MATCHING_PROPERTY_VALUE = "NoMatchingProperty";
    private static final String EMPTY_FILTER = "[]";

    @Test
    public void testFindMultiple() throws ApiException {
        VertexVisibilityHelper helper = new VertexVisibilityHelper();

        List<ClientApiVertex> vertices =
                helper.vertexApi.findMultiple(helper.allVertexIds, false).getVertices();

        assertEquals(2, vertices.size());
        for (ClientApiVertex vertex : vertices) {
            assertEquals(2 + NUM_DEFAULT_PROPERTIES, vertex.getProperties().size());
        }

        final List<String> allVertexIdsIncludingBadOne = new ArrayList<>();
        allVertexIdsIncludingBadOne.addAll(helper.allVertexIds);
        allVertexIdsIncludingBadOne.add("bad");
        Map<String, Boolean> exists = helper.vertexApi.doExist(allVertexIdsIncludingBadOne).getExists();
        assertEquals(4, exists.size()); // should include 2 you can see, one you can't see, and one with a bad id
        for (ClientApiVertex vertex : vertices) {
            assertTrue(vertex.getId() + " should exist", exists.get(vertex.getId()));
        }
        assertFalse("bad should not exist", exists.get("bad"));
    }

    @Test
    public void testSearchVisibleWithQueryString() throws ApiException {
        VertexVisibilityHelper helper = new VertexVisibilityHelper();
        List<ClientApiVertex> vertices;

        // matches all visible
        vertices = helper.vertexApi.vertexSearch(PROPERTY_QUERY_STRING, EMPTY_FILTER, null, null, null, null,
                null).getVertices();

        assertVertexIds(helper.visibleVertexIds, vertices);

        // matches nothing
        vertices = helper.vertexApi.vertexSearch(NO_MATCHING_PROPERTY_VALUE, EMPTY_FILTER, null, null, null,
                null, null).getVertices();

        assertEquals(0, vertices.size());
    }

    @Test
    public void testSearchPublicWithQueryStringForRelated() throws ApiException {
        RelatedVerticesHelper helper = new RelatedVerticesHelper();
        VertexApi vertexApi = helper.vertexApi;
        List<ClientApiVertex> vertices;

        // match single
        vertices = vertexApi.vertexSearch(PROPERTY_QUERY_STRING, EMPTY_FILTER, null, null, null, null,
                helper.getVertexIdForSingleSearch()).getVertices();

        helper.assertRelatedVerticesForSingle(vertices);

        // match multiple
        vertices = vertexApi.vertexSearch(PROPERTY_QUERY_STRING, EMPTY_FILTER, null, null, null, null,
                helper.getVertexIdsForMultipleSearch()).getVertices();

        helper.assertRelatedVerticesForMultiple(vertices);

        // no match
        vertices = vertexApi.vertexSearch(NO_MATCHING_PROPERTY_VALUE, EMPTY_FILTER, null, null, null, null,
                helper.getVertexIdsForMultipleSearch()).getVertices();

        assertEquals(0, vertices.size());
    }

    @Test
    public void testFindRelated() throws ApiException {
        RelatedVerticesHelper helper = new RelatedVerticesHelper();
        VertexApiExt vertexApi = helper.vertexApi;
        List<ClientApiVertex> vertices;

        // single
        vertices = vertexApi.findRelated(helper.getVertexIdForSingleSearch()).getVertices();

        helper.assertRelatedVerticesForSingle(vertices);

        // multiple
        vertices = vertexApi.findRelated(helper.getVertexIdsForMultipleSearch()).getVertices();

        helper.assertRelatedVerticesForMultiple(vertices);
    }

    private class RelatedVerticesHelper {
        final List<String> vertexIds;
        final VertexApiExt vertexApi;

        RelatedVerticesHelper() throws ApiException {
            // Vertex relationships:
            //   0 -> 1, 2
            //   3 -> 0, 1, 4, 5
            //   4 -> 5
            vertexIds = createPublicVertices(6, 1);
            createEdge(vertexIds.get(0), vertexIds.get(1), EDGE_LABEL1);
            createEdge(vertexIds.get(0), vertexIds.get(2), EDGE_LABEL2);
            createEdge(vertexIds.get(3), vertexIds.get(0), EDGE_LABEL1);
            createEdge(vertexIds.get(3), vertexIds.get(1), EDGE_LABEL1);
            createEdge(vertexIds.get(3), vertexIds.get(4), EDGE_LABEL1);
            createEdge(vertexIds.get(4), vertexIds.get(5), EDGE_LABEL1);

            vertexApi = authenticateApiUser().getVertexApi();
        }

        List<String> getVertexIdForSingleSearch() {
            return ImmutableList.of(vertexIds.get(0));
        }

        List<String> getVertexIdsForMultipleSearch() {
            return ImmutableList.of(vertexIds.get(0), vertexIds.get(3));
        }

        void assertRelatedVerticesForSingle(List<ClientApiVertex> actualVertices) {
            assertVertexIds(
                    ImmutableList.of(vertexIds.get(1), vertexIds.get(2), vertexIds.get(3)),
                    actualVertices);
        }

        void assertRelatedVerticesForMultiple(List<ClientApiVertex> actualVertices) {
            // These expected vertices are dependent on the edges set up in createPublicVerticesWithEdges()
            assertVertexIds(
                    ImmutableList.of(
                            vertexIds.get(0), vertexIds.get(1), vertexIds.get(2),
                            vertexIds.get(3), vertexIds.get(4)),
                    actualVertices);
        }
    }

    private class VertexVisibilityHelper {
        final List<String> allVertexIds;
        final List<String> visibleVertexIds;
        final VertexApi vertexApi;

        VertexVisibilityHelper() throws ApiException {
            allVertexIds = createVertices(
                    3, ImmutableList.of("a", "b", "c"),
                    3, ImmutableList.of("x", "y", "z"));
            visibleVertexIds = allVertexIds.subList(0, 2); // only a and b according to user auths below
            vertexApi = authenticateApiUser("a", "b", "x", "y").getVertexApi();
        }
    }

    private LumifyApi authenticateApiUser(String... userAuths) throws ApiException {
        String setupWorkspaceId = setupLumifyApi.getCurrentWorkspaceId(); // capture before switching users
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_2, setupWorkspaceId);
        if (userAuths.length > 0) {
            addUserAuths(lumifyApi, USERNAME_TEST_USER_2, userAuths);
        }
        return lumifyApi;
    }
}
