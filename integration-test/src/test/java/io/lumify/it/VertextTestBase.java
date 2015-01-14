package io.lumify.it;

import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.VertexApi;
import io.lumify.web.clientapi.model.ClientApiElement;
import org.apache.commons.collections.iterators.LoopingIterator;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

public abstract class VertextTestBase extends TestBase {
    protected static final int NUM_DEFAULT_PROPERTIES = 2;

    protected LumifyApi setupLumifyApi;
    protected VertexApi setupVertexApi;

    @Before
    public void setUp() throws ApiException {
        setupLumifyApi = login(USERNAME_TEST_USER_1);
        setupVertexApi = setupLumifyApi.getVertexApi();
        addUserAuths(setupLumifyApi, USERNAME_TEST_USER_1, "a", "b", "c", "x", "y", "z");
    }

    protected List<String> createVertices(
            int numVertices, List<String> vertexVisibilities,
            int numPropertiesPerVertex, List<String> propertyVisibilities
    )
            throws ApiException {
        LoopingIterator vertexVizIterator = new LoopingIterator(vertexVisibilities);
        LoopingIterator propertyVizIterator = new LoopingIterator(propertyVisibilities);
        List<String> vertexIds = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            ClientApiElement vertex = setupVertexApi.create(CONCEPT_TEST_PERSON, (String) vertexVizIterator.next());
            String vertexId = vertex.getId();
            setVertexProperties(numPropertiesPerVertex, propertyVizIterator, vertexId);
            vertexIds.add(vertexId);
        }
        return vertexIds;
    }

    protected void setVertexProperties(int numPropertiesPerVertex, LoopingIterator propertyAuthIterator, String vertexId)
            throws ApiException {
        for (int j = 0; j < numPropertiesPerVertex; j++) {
            setupVertexApi.setProperty(vertexId, "key-firstName-" + j, "http://lumify.io/test#firstName",
                    "First Name " + j, (String) propertyAuthIterator.next(), "", null, null);

        }
    }
}
