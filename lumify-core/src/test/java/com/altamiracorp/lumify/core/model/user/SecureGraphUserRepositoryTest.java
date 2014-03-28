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
public class SecureGraphUserRepositoryTest {
    @Mock
    private OntologyRepository ontologyRepository;

    @Mock
    private Concept userConcept;

    private AuthorizationRepository authorizationRepository;
    private SecureGraphUserRepository secureGraphUserRepository;

    @Before
    public void setup() {
        InMemoryGraphConfiguration config = new InMemoryGraphConfiguration(new HashMap());
        authorizationRepository = new InMemoryAuthorizationRepository();
        authorizationRepository.addAuthorizationToGraph(LumifyVisibility.VISIBILITY_STRING.toString());
        when(ontologyRepository.getOrCreateConcept((Concept) isNull(), eq(UserRepository.LUMIFY_USER_CONCEPT_ID), anyString())).thenReturn(userConcept);
        when(userConcept.getId()).thenReturn(UserRepository.LUMIFY_USER_CONCEPT_ID);

        secureGraphUserRepository = new SecureGraphUserRepository();
        secureGraphUserRepository.setGraph(new InMemoryGraph(config, new UUIDIdGenerator(config.getConfig()), new DefaultSearchIndex(config.getConfig())));
        secureGraphUserRepository.setAuthorizationRepository(authorizationRepository);
    }

    @Test
    public void testAddUser() {
        secureGraphUserRepository.addUser("testUser", "testPassword", new String[]{"auth1", "auth2"});

        Vertex userVertex = secureGraphUserRepository.findByUserName("testUser");
        assertEquals("testUser", UserLumifyProperties.USERNAME.getPropertyValue(userVertex));
    }
}
