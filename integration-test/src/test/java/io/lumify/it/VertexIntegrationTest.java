package io.lumify.it;

import com.google.common.collect.ImmutableList;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.VertexApi;
import io.lumify.web.clientapi.model.ClientApiVertex;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class VertexIntegrationTest extends VertextTestBase {
    @Test
    public void testFindMultiple() throws Exception {
        final List<String> allVertexIds = createVertices(
                3, ImmutableList.of("a", "b", "c"),
                3, ImmutableList.of("x", "y", "z"));
        String setupWorkspaceId = setupLumifyApi.getCurrentWorkspaceId();
        LumifyApi lumifyApi = login(USERNAME_TEST_USER_2);
        addUserAuths(lumifyApi, USERNAME_TEST_USER_2, "a", "b", "x", "y", setupWorkspaceId);
        final VertexApi vertexApi = lumifyApi.getVertexApi();

        List<ClientApiVertex> vertices = vertexApi.findMultiple(allVertexIds, false).getVertices();

        assertEquals(2, vertices.size());
        for (ClientApiVertex vertex : vertices) {
            assertEquals(2 + NUM_DEFAULT_PROPERTIES, vertex.getProperties().size());
        }
    }
}
