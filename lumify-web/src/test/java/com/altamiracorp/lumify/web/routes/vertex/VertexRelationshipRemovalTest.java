package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexRelationshipRemovalTest extends RouteTestBase {
    private VertexRelationshipRemoval vertexRelationshipRemoval;

    @Mock
    private GraphRepository mockGraphRepository;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        vertexRelationshipRemoval = new VertexRelationshipRemoval(mockGraphRepository);
    }

    @Test
    public void testHandle() throws Exception {
        when(mockRequest.getParameter("sourceId")).thenReturn("sourceId");
        when(mockRequest.getParameter("targetId")).thenReturn("targetId");
        when(mockRequest.getParameter("label")).thenReturn("label");

        vertexRelationshipRemoval.handle(mockRequest, mockResponse, mockHandlerChain);
        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
        assertTrue(response.getBoolean("success"));
    }
}
