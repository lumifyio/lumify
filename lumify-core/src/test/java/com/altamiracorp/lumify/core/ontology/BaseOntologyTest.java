package com.altamiracorp.lumify.core.ontology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaseOntologyTest {
    @Mock
    User user;
    @Mock
    Concept rootConcept;
    @Mock
    Concept artifactConcept;
    @Mock
    Concept imageConcept;
    @Mock
    Concept entityConcept;
    @Mock
    Vertex entityVertex;
    @Mock
    OntologyRepository ontologyRepository;
    @Mock
    Graph graph;

    private BaseOntology baseOntology;

    @Before
    public void setUp() {
        baseOntology = new BaseOntology(ontologyRepository, graph);
        when(entityConcept.getVertex()).thenReturn(entityVertex);
    }

    @Test
    public void testDefineOntology() {
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"))).thenReturn(entityConcept);

        baseOntology.defineOntology(user);

        verify(ontologyRepository, times(2)).addPropertyTo(eq(rootConcept.getVertex()), anyString(), anyString(), any(PropertyType.class));
        verify(ontologyRepository, times(2)).addPropertyTo(eq(entityConcept.getVertex()), anyString(), anyString(), any(PropertyType.class));

        // TODO rewrite this test for secure graph!!!
//        verify(entityConcept).setProperty(PropertyName.GLYPH_ICON.toString(), "rowKey", (Visibility) any());
    }

    @Test
    public void testIsOntologyDefinedTrue() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString())).thenReturn(rootConcept);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(true, result);
    }

    @Test
    public void testIsOntologyDefinedFalse() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString())).thenReturn(null);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test(expected = RuntimeException.class)
    public void testIsOntologyDefinedException() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString())).thenThrow(new RuntimeException("test", new Throwable("testing exception")));
        baseOntology.isOntologyDefined(user);
    }

    @Test
    public void testIsOntologyDefinedExceptionWithFalse() {
        when(ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY)).thenThrow(new RuntimeException("ontologyTitle", new Throwable("testing exception")));
        assertFalse(baseOntology.isOntologyDefined(user));
    }

    @Test
    public void testInitializeWhenUndefined() {
        when(ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY)).thenReturn(null);
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"))).thenReturn(entityConcept);

        baseOntology.initialize(user);
    }

    @Test
    public void testInitializeWhenDefined() {
        when(ontologyRepository.getConceptByName(OntologyRepository.TYPE_ENTITY)).thenReturn(rootConcept);
        baseOntology.initialize(user);
    }
}
