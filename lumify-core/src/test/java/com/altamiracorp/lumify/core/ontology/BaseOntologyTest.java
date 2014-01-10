package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.model.GraphSession;
import com.altamiracorp.lumify.core.model.ontology.*;
import com.altamiracorp.lumify.core.model.resources.ResourceRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanLabel;
import com.thinkaurelius.titan.core.TypeMaker;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BaseOntologyTest {
    @Mock
    SystemUser user;
    @Mock
    Concept rootConcept;
    @Mock
    Concept artifactConcept;
    @Mock
    Concept imageConcept;
    @Mock
    Concept entityConcept;
    @Mock
    OntologyRepository ontologyRepository;
    @Mock
    ResourceRepository resourceRepository;
    @Mock
    GraphSession graphSession;
    @Mock
    TitanGraph titanGraph;
    @Mock
    TitanKey titanKey;
    @Mock
    TitanLabel titanLabel;

    private BaseOntology baseOntology;
    @Before
    public void setUp() {
        baseOntology = new BaseOntology(ontologyRepository, resourceRepository, graphSession);
    }

    @Test
    public void testDefineOntology() {
        when(graphSession.getGraph()).thenReturn(titanGraph);

        when(titanGraph.getType("_conceptType")).thenReturn(titanKey);
        when(titanGraph.getType("_dataType")).thenReturn(titanKey);
        when(titanGraph.getType("ontologyTitle")).thenReturn(titanKey);
        when(titanGraph.getType("hasProperty")).thenReturn(titanLabel);
        when(titanGraph.getType("hasEdge")).thenReturn(titanLabel);
        when(titanGraph.getType("isA")).thenReturn(titanLabel);
        when(titanGraph.getType("relationshipType")).thenReturn(titanKey);
        when(titanGraph.getType("_timeStamp")).thenReturn(titanKey);
        when(titanGraph.getType("_rawHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("_textHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("highlightedTextHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("_detectedObjects")).thenReturn(titanKey);
        when(titanGraph.getType("_subType")).thenReturn(titanKey);
        when(titanGraph.getType("displayName")).thenReturn(titanKey);
        when(titanGraph.getType("displayType")).thenReturn(titanKey);
        when(titanGraph.getType("title")).thenReturn(titanKey);
        when(titanGraph.getType("_glyphIcon")).thenReturn(titanKey);
        when(titanGraph.getType("_mapGlyphIcon")).thenReturn(titanKey);
        when(titanGraph.getType("_color")).thenReturn(titanKey);
        when(titanGraph.getType("geoLocation")).thenReturn(titanKey);
        when(titanGraph.getType("_geoLocationDescription")).thenReturn(titanKey);
        when(titanGraph.getType("publishedDate")).thenReturn(titanKey);
        when(titanGraph.getType("source")).thenReturn(titanKey);
        when(titanGraph.getType("author")).thenReturn(titanKey);
        when(titanGraph.getType("_rowKey")).thenReturn(titanKey);

        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"), eq(user))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"), eq(user))).thenReturn(entityConcept);

        when(resourceRepository.importFile(any(InputStream.class), anyString(), eq(user))).thenReturn("rowKey");

        baseOntology.defineOntology(user);

        verify(ontologyRepository, times(2)).addPropertyTo(eq(rootConcept), anyString(), anyString(), any(PropertyType.class), eq(user));
        verify(ontologyRepository, times(2)).addPropertyTo(eq(entityConcept), anyString(), anyString(), any(PropertyType.class), eq(user));

        verify(entityConcept).setProperty(PropertyName.GLYPH_ICON, "rowKey");
    }

    @Test
    public void testIsOntologyDefinedTrue() {
        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenReturn(rootConcept);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(true, result);
    }

    @Test
    public void testIsOntologyDefinedFalse() {
        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenReturn(null);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test (expected = RuntimeException.class)
    public void testIsOntologyDefinedException() {
        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenThrow(new RuntimeException("test", new Throwable("testing exception")));
        baseOntology.isOntologyDefined(user);
    }

    @Test 
    public void testIsOntologyDefinedExceptionWithFalse() {
        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenThrow(new RuntimeException("ontologyTitle", new Throwable("testing exception")));
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test
    public void testInitializeWhenUndefined() {
        when(graphSession.getGraph()).thenReturn(titanGraph);

        when(titanGraph.getType("_conceptType")).thenReturn(titanKey);
        when(titanGraph.getType("_dataType")).thenReturn(titanKey);
        when(titanGraph.getType("ontologyTitle")).thenReturn(titanKey);
        when(titanGraph.getType("hasProperty")).thenReturn(titanLabel);
        when(titanGraph.getType("hasEdge")).thenReturn(titanLabel);
        when(titanGraph.getType("isA")).thenReturn(titanLabel);
        when(titanGraph.getType("relationshipType")).thenReturn(titanKey);
        when(titanGraph.getType("_timeStamp")).thenReturn(titanKey);
        when(titanGraph.getType("_rawHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("_textHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("highlightedTextHdfsPath")).thenReturn(titanKey);
        when(titanGraph.getType("_detectedObjects")).thenReturn(titanKey);
        when(titanGraph.getType("_subType")).thenReturn(titanKey);
        when(titanGraph.getType("displayName")).thenReturn(titanKey);
        when(titanGraph.getType("displayType")).thenReturn(titanKey);
        when(titanGraph.getType("title")).thenReturn(titanKey);
        when(titanGraph.getType("_glyphIcon")).thenReturn(titanKey);
        when(titanGraph.getType("_mapGlyphIcon")).thenReturn(titanKey);
        when(titanGraph.getType("_color")).thenReturn(titanKey);
        when(titanGraph.getType("geoLocation")).thenReturn(titanKey);
        when(titanGraph.getType("_geoLocationDescription")).thenReturn(titanKey);
        when(titanGraph.getType("publishedDate")).thenReturn(titanKey);
        when(titanGraph.getType("source")).thenReturn(titanKey);
        when(titanGraph.getType("author")).thenReturn(titanKey);
        when(titanGraph.getType("_rowKey")).thenReturn(titanKey);

        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenReturn(null);
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"), eq(user))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"), eq(user))).thenReturn(entityConcept);

        when(resourceRepository.importFile(any(InputStream.class), anyString(), eq(user))).thenReturn("rowKey");
        baseOntology.initialize(user);
        verify(titanGraph, times(22)).getType(anyString());
    }

    @Test
    public void testInitializeWhenDefined() {
        when(ontologyRepository.getConceptByName(ontologyRepository.ENTITY.toString(), user)).thenReturn(rootConcept);
        baseOntology.initialize(user);
        verify(titanGraph, times(0)).getType(anyString());
    }
}
