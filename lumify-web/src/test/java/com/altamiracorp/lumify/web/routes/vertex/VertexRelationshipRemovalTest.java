package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class VertexRelationshipRemovalTest extends RouteTestBase {
    private VertexRelationshipRemoval vertexRelationshipRemoval;

    @Mock
    private Graph mockGraph;
    @Mock
    private AuditRepository mockAuditRepository;
    @Mock
    private OntologyRepository mockOntologyRepositiory;
    @Mock
    private Vertex mockSourceVertex;
    @Mock
    private Vertex mockDestVertex;
    @Mock
    private UserRepository userRepository;

    @Override
    @Before
    public void setUp() throws Exception {
//        super.setUp();
        vertexRelationshipRemoval = new VertexRelationshipRemoval(mockGraph, mockAuditRepository, mockOntologyRepositiory, userRepository);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!

//        when(mockRequest.getParameter("sourceId")).thenReturn("sourceId");
//        when(mockRequest.getParameter("targetId")).thenReturn("targetId");
//        when(mockRequest.getParameter("label")).thenReturn("label");
//
//        when(mockGraph.getVertex("sourceId", mockUser.getAuthorizations())).thenReturn(mockSourceVertex);
//        when(mockGraph.getVertex("targetId", mockUser.getAuthorizations())).thenReturn(mockDestVertex);
//        when(mockOntologyRepositiory.getDisplayNameForLabel("label", mockUser)).thenReturn("label");
//
//        vertexRelationshipRemoval.handle(mockRequest, mockResponse, mockHandlerChain);
//        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
//        assertTrue(response.getBoolean("success"));
//
//        verify(mockAuditRepository, times(1)).auditRelationship(AuditAction.DELETE.toString(), mockSourceVertex, mockDestVertex, "label", "", "", mockUser);
    }
}
