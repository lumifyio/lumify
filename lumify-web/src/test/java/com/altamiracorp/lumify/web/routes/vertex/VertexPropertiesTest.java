package com.altamiracorp.lumify.web.routes.vertex;

import com.altamiracorp.lumify.core.model.graph.GraphRepository;
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
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VertexPropertiesTest extends RouteTestBase {
    private final String ID = "40004";
    private final String TITLE = "testVertex";
    private final String AUTHOR = "lumify";
    private final String SUBTYPE = "28";

    private VertexProperties vertexProperties;

    @Mock
    private GraphRepository mockGraphRepository;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        vertexProperties = new VertexProperties(mockGraphRepository);
    }

    @Test
    public void testHandle() throws Exception {
        when(mockRequest.getAttribute("graphVertexId")).thenReturn(ID);

        Map<String, String> properties = new HashMap<String, String>();
        properties.put(PropertyName.TITLE.toString(), TITLE);
        properties.put(PropertyName.AUTHOR.toString(), AUTHOR);
        properties.put(PropertyName.CONCEPT_TYPE.toString(), SUBTYPE);

        when(mockGraphRepository.getVertexProperties(ID, mockUser)).thenReturn(properties);

        vertexProperties.handle(mockRequest, mockResponse, mockHandlerChain);

        JSONObject response = new JSONObject(responseStringWriter.getBuffer().toString());
        assertEquals(ID, response.getString("id"));
        assertTrue(response.getJSONObject("properties").length() > 0);
        assertEquals(TITLE, response.getJSONObject("properties").getString(PropertyName.TITLE.toString()));
        assertEquals(SUBTYPE, response.getJSONObject("properties").getString(PropertyName.CONCEPT_TYPE.toString()));
        assertEquals(AUTHOR, response.getJSONObject("properties").getString(PropertyName.AUTHOR.toString()));
    }
}
