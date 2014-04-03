package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface OntologyRepository {
    public static final String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    public static final String ROOT_CONCEPT_IRI = "http://lumify.io#root";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String VISIBILITY_STRING = "ontology";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);

    void clearCache();

    void storeOntologyFile(InputStream in, IRI documentIRI);

    List<OWLOntology> loadOntologyFiles(OWLOntologyManager m, OWLOntologyLoaderConfiguration config, IRI excludedIRI) throws OWLOntologyCreationException, IOException;

    Iterable<Relationship> getRelationshipLabels();

    String getDisplayNameForLabel(String relationshipLabel);

    List<OntologyProperty> getProperties();

    OntologyProperty getProperty(String propertyName);

    Relationship getRelationshipByVertexId(String relationshipId);

    Iterable<Concept> getConcepts();

    Iterable<Concept> getConceptsWithProperties();

    Concept getEntityConcept();

    Concept getParentConcept(Concept concept);

    Concept getParentConcept(String conceptId);

    Concept getConceptByVertexId(String conceptVertexId);

    List<Concept> getConceptAndChildrenByVertexId(String conceptId);

    List<Concept> getAllLeafNodesByConcept(Concept concept);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName);

    OntologyProperty addPropertyTo(Vertex vertex, String propertyId, String displayName, PropertyType dataType, boolean userVisible);

    Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipId, String displayName);

    void resolvePropertyIds(JSONArray filterJson) throws JSONException;
}
