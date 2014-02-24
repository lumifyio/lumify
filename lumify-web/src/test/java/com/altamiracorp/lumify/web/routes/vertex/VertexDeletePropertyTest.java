package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import com.altamiracorp.securegraph.Graph;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexDeletePropertyTest extends RouteTestBase {
    private final String AUTHOR = "foo";
    private final String CONCEPT_TYPE = "28";
    private VertexDeleteProperty vertexDeleteProperty;

    @Mock
    private Graph mockGraph;
    @Mock
    private AuditRepository mockAuditRepository;
    @Mock
    private OntologyProperty mockProperty;
    @Mock
    private UserRepository userRepository;

    @Before
    @Override
    public void setUp() throws Exception {
//        super.setUp();
        vertexDeleteProperty = new VertexDeleteProperty(mockGraph, mockAuditRepository, userRepository);
    }

    @Test(expected = RuntimeException.class)
    public void testHandleWithNullProperty() throws Exception {
        when(mockRequest.getAttribute("graphVertexId")).thenReturn("");
        when(mockRequest.getParameter("propertyName")).thenReturn("");
        vertexDeleteProperty.handle(mockRequest, mockResponse, mockHandlerChain);
    }

    @Test
    public void testHandle() throws Exception {
        // TODO rewrite this test for secure graph!!!
//        when(mockRequest.getParameter("propertyName")).thenReturn(PropertyName.TITLE.toString());
//        when(mockOntologyRepository.getProperty(PropertyName.TITLE.toString(), mockUser)).thenReturn(mockProperty);
//        when(mockRequest.getAttribute("graphVertexId")).thenReturn("40004");
//
//        GraphVertex vertex = new InMemoryGraphVertex("40004");
//        vertex.setProperty(PropertyName.TITLE.toString(), "testVertex");
//
//        Map<String, String> properties = new HashMap<String, String>();
//        properties.put(PropertyName.AUTHOR.toString(), AUTHOR);
//        properties.put(PropertyName.CONCEPT_TYPE.toString(), CONCEPT_TYPE);
//
//        when(mockGraphRepository.findVertex(vertex.getId(), mockUser)).thenReturn(vertex);
//        when(mockGraphRepository.getVertexProperties(vertex.getId(), mockUser)).thenReturn(properties);
//
//        vertexDeleteProperty.handle(mockRequest, mockResponse, mockHandlerChain);
//
//        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
//        JSONObject responseProperties = response.getJSONObject("properties");
//        assertEquals(PropertyName.TITLE.toString(), response.getString("deletedProperty"));
//        assertEquals(vertex.getId(), response.getJSONObject("vertex").getString("graphVertexId"));
//        assertTrue(responseProperties.length() > 0);
//        assertEquals(AUTHOR, responseProperties.getString(PropertyName.AUTHOR.toString()));
//        assertEquals(CONCEPT_TYPE, responseProperties.getString(PropertyName.CONCEPT_TYPE.toString()));
//
//        verify(mockAuditRepository, times(1)).auditEntityProperty(AuditAction.DELETE.toString(), vertex, PropertyName.TITLE.toString(), "", "", mockUser);
    }
}
