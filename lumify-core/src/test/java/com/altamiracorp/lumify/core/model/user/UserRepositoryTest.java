package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.id.UUIDIdGenerator;
import com.altamiracorp.securegraph.inmemory.InMemoryGraph;
import com.altamiracorp.securegraph.inmemory.InMemoryGraphConfiguration;
import com.altamiracorp.securegraph.search.DefaultSearchIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserRepositoryTest {
    private Graph graph;

    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept userConcept;

    private AuthorizationRepository authorizationRepository;
    private UserRepository userRepository;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        graph = new InMemoryGraph(config, new UUIDIdGenerator(config.getConfig()), new DefaultSearchIndex(config.getConfig()));
        authorizationRepository = new InMemoryAuthorizationRepository();
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.VISIBILITY_STRING.toString());
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq(UserRepository.LUMIFY_USER_CONCEPT_ID), anyString())).thenReturn(userConcept);
        when(userConcept.getId()).thenReturn(UserRepository.LUMIFY_USER_CONCEPT_ID);

        userRepository = new UserRepository(graph, ontologyRepository, authorizationRepository);
    }

    @Test
    public void testAddUser() {
        userRepository.addUser("testUser", "testPassword", new String[]{"auth1", "auth2"});

        Vertex userVertex = userRepository.findByUserName("testUser");
        assertEquals("testUser", UserLumifyProperties.USERNAME.getPropertyValue(userVertex));
    }
}
