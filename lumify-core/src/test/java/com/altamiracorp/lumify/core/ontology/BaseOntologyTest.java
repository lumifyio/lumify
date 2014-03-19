package com.altamiracorp.lumify.core.ontology;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.when;

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
    }

    @Test
    public void testIsOntologyDefinedTrue() {
        when(ontologyRepository.getConceptById(ontologyRepository.ROOT_CONCEPT_IRI.toString())).thenReturn(rootConcept);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(true, result);
    }

    @Test
    public void testIsOntologyDefinedFalse() {
        when(ontologyRepository.getConceptById(ontologyRepository.ROOT_CONCEPT_IRI.toString())).thenReturn(null);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test(expected = RuntimeException.class)
    public void testIsOntologyDefinedException() {
        when(ontologyRepository.getConceptById(ontologyRepository.ROOT_CONCEPT_IRI.toString())).thenThrow(new RuntimeException("test", new Throwable("testing exception")));
        baseOntology.isOntologyDefined(user);
    }

    @Test
    public void testIsOntologyDefinedExceptionWithFalse() {
        when(ontologyRepository.getConceptById(OntologyRepository.ROOT_CONCEPT_IRI)).thenThrow(new RuntimeException("ontologyTitle", new Throwable("testing exception")));
        assertFalse(baseOntology.isOntologyDefined(user));
    }

    @Test
    public void testInitializeWhenUndefined() {
        when(ontologyRepository.getConceptById(OntologyRepository.ROOT_CONCEPT_IRI)).thenReturn(null);
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"))).thenReturn(entityConcept);

        baseOntology.initialize(user);
    }

    @Test
    public void testInitializeWhenDefined() {
        when(ontologyRepository.getConceptById(OntologyRepository.ROOT_CONCEPT_IRI)).thenReturn(rootConcept);
        baseOntology.initialize(user);
    }
}
