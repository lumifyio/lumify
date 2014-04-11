package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.security.LumifyVisibility;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public interface OntologyRepository {
    public static final String ENTITY_CONCEPT_IRI = "http://www.w3.org/2002/07/owl#Thing";
    public static final String ROOT_CONCEPT_IRI = "http://lumify.io#root";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String VISIBILITY_STRING = "ontology";
    public static final LumifyVisibility VISIBILITY = new LumifyVisibility(VISIBILITY_STRING);

    void clearCache();

    Iterable<Relationship> getRelationshipLabels();

    Iterable<OntologyProperty> getProperties();

    Iterable<Concept> getConcepts();

    String getDisplayNameForLabel(String relationshipIRI);

    OntologyProperty getProperty(String propertyIRI);

    Relationship getRelationshipByIRI(String relationshipIRI);

    Iterable<Concept> getConceptsWithProperties();

    Concept getEntityConcept();

    Concept getParentConcept(Concept concept);

    Concept getConceptByIRI(String conceptIRI);

    List<Concept> getConceptAndChildrenByIRI(String conceptIRI);

    List<Concept> getAllLeafNodesByConcept(Concept concept);

    Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName);

    Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipIRI, String displayName);

    void resolvePropertyIds(JSONArray filterJson) throws JSONException;

    void importFile(File inFile, IRI documentIRI) throws Exception;

    void importFile(InputStream in, IRI documentIRI, File inDir) throws Exception;

    void exportOntology(OutputStream out, IRI documentIRI) throws Exception;
}
