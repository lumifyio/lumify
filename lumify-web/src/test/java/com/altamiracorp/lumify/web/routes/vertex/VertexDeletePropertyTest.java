package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.audit.AuditAction;
import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Property;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.web.routes.RouteTestBase;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VertexDeletePropertyTest extends RouteTestBase {
    private final String AUTHOR = "foo";
    private final String CONCEPT_TYPE = "28";
    private VertexDeleteProperty vertexDeleteProperty;

    @Mock
    private OntologyRepository mockOntologyRepository;
    @Mock
    private GraphRepository mockGraphRepository;
    @Mock
    private AuditRepository mockAuditRepository;
    @Mock
    private Property mockProperty;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        vertexDeleteProperty = new VertexDeleteProperty(mockOntologyRepository, mockGraphRepository, mockAuditRepository);
    }

    @Test(expected = RuntimeException.class)
    public void testHandleWithNullProperty() throws Exception {
        when(mockRequest.getAttribute("graphVertexId")).thenReturn("");
        when(mockRequest.getParameter("propertyName")).thenReturn("");
        when(mockOntologyRepository.getProperty(PropertyName.TITLE.toString(), mockUser)).thenReturn(null);
        vertexDeleteProperty.handle(mockRequest, mockResponse, mockHandlerChain);
    }

    @Test
    public void testHandle() throws Exception {
        when(mockRequest.getParameter("propertyName")).thenReturn(PropertyName.TITLE.toString());
        when(mockOntologyRepository.getProperty(PropertyName.TITLE.toString(), mockUser)).thenReturn(mockProperty);
        when(mockRequest.getAttribute("graphVertexId")).thenReturn("40004");

        GraphVertex vertex = new InMemoryGraphVertex("40004");
        vertex.setProperty(PropertyName.TITLE.toString(), "testVertex");

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyName.AUTHOR.toString(), AUTHOR);
        properties.put(PropertyName.CONCEPT_TYPE.toString(), CONCEPT_TYPE);

        when(mockGraphRepository.findVertex(vertex.getId(), mockUser)).thenReturn(vertex);
        when(mockGraphRepository.getVertexProperties(vertex.getId(), mockUser)).thenReturn(properties);

        vertexDeleteProperty.handle(mockRequest, mockResponse, mockHandlerChain);

        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
        JSONObject responseProperties = response.getJSONObject("properties");
        assertEquals(PropertyName.TITLE.toString(), response.getString("deletedProperty"));
        assertEquals(vertex.getId(), response.getJSONObject("vertex").getString("graphVertexId"));
        assertTrue(responseProperties.length() > 0);
        assertEquals(AUTHOR, responseProperties.getString(PropertyName.AUTHOR.toString()));
        assertEquals(CONCEPT_TYPE, responseProperties.getString(PropertyName.CONCEPT_TYPE.toString()));

        verify(mockAuditRepository, times(1)).auditEntityProperties(AuditAction.DELETE.toString(), vertex, PropertyName.TITLE.toString(), "", "", mockUser);
    }
}
