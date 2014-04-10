package com.altamiracorp.lumify.sql.model.ontology;

import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyProperty;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.Relationship;
import org.json.JSONArray;
import org.json.JSONException;
import org.semanticweb.owlapi.model.IRI;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

public class SqlOntologyRepository implements OntologyRepository {
    @Override
    public void init(Map config) {

    }

    @Override
    public void clearCache() {

    }

    @Override
    public Iterable<Relationship> getRelationshipLabels() {
        return null;
    }

    @Override
    public Iterable<OntologyProperty> getProperties() {
        return null;
    }

    @Override
    public Iterable<Concept> getConcepts() {
        return null;
    }

    @Override
    public String getDisplayNameForLabel(String relationshipIRI) {
        return null;
    }

    @Override
    public OntologyProperty getProperty(String propertyIRI) {
        return null;
    }

    @Override
    public Relationship getRelationshipByIRI(String relationshipIRI) {
        return null;
    }

    @Override
    public Iterable<Concept> getConceptsWithProperties() {
        return null;
    }

    @Override
    public Concept getEntityConcept() {
        return null;
    }

    @Override
    public Concept getParentConcept(Concept concept) {
        return null;
    }

    @Override
    public Concept getConceptByIRI(String conceptIRI) {
        return null;
    }

    @Override
    public List<Concept> getConceptAndChildrenByIRI(String conceptIRI) {
        return null;
    }

    @Override
    public List<Concept> getAllLeafNodesByConcept(Concept concept) {
        return null;
    }

    @Override
    public Concept getOrCreateConcept(Concept parent, String conceptIRI, String displayName) {
        return null;
    }

    @Override
    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipIRI, String displayName) {
        return null;
    }

    @Override
    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {

    }

    @Override
    public void importFile(File inFile, IRI documentIRI) throws Exception {

    }

    @Override
    public void importFile(InputStream in, IRI documentIRI, File inDir) throws Exception {

    }

    @Override
    public void exportOntology(OutputStream out, IRI documentIRI) throws Exception {

    }
}
