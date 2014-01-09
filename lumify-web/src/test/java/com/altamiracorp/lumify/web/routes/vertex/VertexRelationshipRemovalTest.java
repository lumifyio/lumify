package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VertexRelationshipRemovalTest extends RouteTestBase {
    private VertexRelationshipRemoval vertexRelationshipRemoval;

    @Mock
    private GraphRepository mockGraphRepository;
    @Mock
    private AuditRepository mockAuditRepository;
    @Mock
    private OntologyRepository mockOntologyRepositiory;
    @Mock
    private GraphVertex mockSourceVertex;
    @Mock
    private GraphVertex mockDestVertex;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        vertexRelationshipRemoval = new VertexRelationshipRemoval(mockGraphRepository, mockAuditRepository, mockOntologyRepositiory);
    }

    @Test
    public void testHandle() throws Exception {
        when(mockRequest.getParameter("sourceId")).thenReturn("sourceId");
        when(mockRequest.getParameter("targetId")).thenReturn("targetId");
        when(mockRequest.getParameter("label")).thenReturn("label");

        when(mockGraphRepository.findVertex("sourceId", mockUser)).thenReturn(mockSourceVertex);
        when(mockGraphRepository.findVertex("targetId", mockUser)).thenReturn(mockDestVertex);
        when (mockOntologyRepositiory.getDisplayNameForLabel("label", mockUser)).thenReturn("label");

        vertexRelationshipRemoval.handle(mockRequest, mockResponse, mockHandlerChain);
        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
        assertTrue(response.getBoolean("success"));

        verify(mockAuditRepository, times(1)).auditRelationships(AuditAction.DELETE.toString(), mockSourceVertex, mockDestVertex, "label", "", "", mockUser);
    }
}
