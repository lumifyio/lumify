package com.altamiracorp.lumify.core.ontology;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.PropertyType;
import com.altamiracorp.lumify.core.model.resources.ResourceRepository;
import com.altamiracorp.lumify.core.user.SystemUser;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Visibility;
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
    Graph graph;

    private BaseOntology baseOntology;

    @Before
    public void setUp() {
        baseOntology = new BaseOntology(ontologyRepository, resourceRepository, graph);
    }

    @Test
    public void testDefineOntology() {
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"), eq(user))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"), eq(user))).thenReturn(entityConcept);

        when(resourceRepository.importFile(any(InputStream.class), anyString(), eq(user))).thenReturn("rowKey");

        baseOntology.defineOntology(user);

        verify(ontologyRepository, times(2)).addPropertyTo(eq(rootConcept.getVertex()), anyString(), anyString(), any(PropertyType.class), eq(user));
        verify(ontologyRepository, times(2)).addPropertyTo(eq(entityConcept.getVertex()), anyString(), anyString(), any(PropertyType.class), eq(user));

        verify(entityConcept).setProperty(PropertyName.GLYPH_ICON.toString(), "rowKey", (Visibility) any());
    }

    @Test
    public void testIsOntologyDefinedTrue() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenReturn(rootConcept);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(true, result);
    }

    @Test
    public void testIsOntologyDefinedFalse() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenReturn(null);
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test(expected = RuntimeException.class)
    public void testIsOntologyDefinedException() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenThrow(new RuntimeException("test", new Throwable("testing exception")));
        baseOntology.isOntologyDefined(user);
    }

    @Test
    public void testIsOntologyDefinedExceptionWithFalse() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenThrow(new RuntimeException("ontologyTitle", new Throwable("testing exception")));
        boolean result = baseOntology.isOntologyDefined(user);
        assertEquals(false, result);
    }

    @Test
    public void testInitializeWhenUndefined() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenReturn(null);
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq("rootConcept"), eq("rootConcept"), eq(user))).thenReturn(rootConcept);
        when(ontologyRepository.getOrCreateConcept(eq(rootConcept), anyString(), eq("Entity"), eq(user))).thenReturn(entityConcept);

        when(resourceRepository.importFile(any(InputStream.class), anyString(), eq(user))).thenReturn("rowKey");
        baseOntology.initialize(user);
    }

    @Test
    public void testInitializeWhenDefined() {
        when(ontologyRepository.getConceptByName(ontologyRepository.TYPE_ENTITY.toString(), user)).thenReturn(rootConcept);
        baseOntology.initialize(user);
    }
}
