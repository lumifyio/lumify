package io.lumify.it;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import io.lumify.web.clientapi.LumifyApi;
import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.EdgeApi;
import io.lumify.web.clientapi.codegen.VertexApi;
import io.lumify.web.clientapi.model.ClientApiElement;
import io.lumify.web.clientapi.model.ClientApiVertex;
import org.apache.commons.collections.iterators.LoopingIterator;
import org.junit.Before;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class VertextTestBase extends TestBase {
    protected static final int NUM_DEFAULT_PROPERTIES = 2;
    protected static final List<String> PUBLIC_VISIBILITY = ImmutableList.of("");
    protected static final String PROPERTY_NAME = "http://lumify.io/test#firstName";
    protected static final String PROPERTY_KEY_PREFIX = "key-firstName-";
    protected static final String PROPERTY_VALUE_PREFIX = "First Name ";
    protected static final String EDGE_LABEL1 = "http://lumify.io/test#worksFor";
    protected static final String EDGE_LABEL2 = "http://lumify.io/test#sibling";

    protected LumifyApi setupLumifyApi;
    protected VertexApi setupVertexApi;
    protected EdgeApi setupEdgeApi;

    @Before
    public void setUp() throws ApiException {
        setupLumifyApi = login(USERNAME_TEST_USER_1);
        setupVertexApi = setupLumifyApi.getVertexApi();
        setupEdgeApi = setupLumifyApi.getEdgeApi();
        addUserAuths(setupLumifyApi, USERNAME_TEST_USER_1, "a", "b", "c", "d", "e", "f", "x", "y", "z");
    }

    protected List<String> createVertices(int numVertices, List<String> vertexVisibilities,
                                          int numPropertiesPerVertex, List<String> propertyVisibilities)
            throws ApiException {
        LoopingIterator vertexVizIterator = new LoopingIterator(vertexVisibilities);
        LoopingIterator propertyVizIterator = new LoopingIterator(propertyVisibilities);
        List<String> vertexIds = new ArrayList<>();
        for (int i = 0; i < numVertices; i++) {
            ClientApiElement vertex = setupVertexApi.create(TestOntology.CONCEPT_PERSON, (String) vertexVizIterator.next());
            String vertexId = vertex.getId();
            setVertexProperties(numPropertiesPerVertex, propertyVizIterator, vertexId);
            vertexIds.add(vertexId);
        }
        return vertexIds;
    }

    protected List<String> createPublicVertices(int numVertices, int numPropertiesPerVertex) throws ApiException {
        return createVertices(numVertices, PUBLIC_VISIBILITY, numPropertiesPerVertex, PUBLIC_VISIBILITY);
    }

    protected void setVertexProperties(int numPropertiesPerVertex, LoopingIterator propertyAuthIterator, String vertexId)
            throws ApiException {
        for (int j = 0; j < numPropertiesPerVertex; j++) {
            setupVertexApi.setProperty(vertexId, PROPERTY_KEY_PREFIX + j, PROPERTY_NAME, PROPERTY_VALUE_PREFIX + j,
                    (String) propertyAuthIterator.next(), "", null, null);

        }
    }

    protected void createEdge(String sourceVertexId, String destVertexId, String edgeLabel) throws ApiException {
        setupEdgeApi.create(sourceVertexId, destVertexId, edgeLabel, "", "ok", "{}");
    }

    protected void assertVertexIds(List<String> expectedVertexIds, List<ClientApiVertex> actualVertices) {
        List<String> expectedIds = new ArrayList<>(expectedVertexIds);
        List<String> actualIds = new ArrayList<>(Lists.transform(actualVertices, new Function<ClientApiVertex, String>() {
            public String apply(ClientApiVertex vertex) {
                return vertex.getId();
            }
        }));
        Collections.sort(expectedIds);
        Collections.sort(actualIds);

        assertEquals(expectedIds, actualIds);
    }
}
