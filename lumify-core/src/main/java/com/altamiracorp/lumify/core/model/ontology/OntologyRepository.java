package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.util.FilterIterable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class OntologyRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(OntologyRepository.class);
    private static final Authorizations AUTHORIZATIONS = new Authorizations();
    private static final Visibility DEFAULT_VISIBILITY = new Visibility("");
    private Graph graph;
    public static final String ROOT_CONCEPT_NAME = "rootConcept";
    public static final String RELATIONSHIP_CONCEPT = "relationship";
    public static final String CONCEPT = "concept";
    public static final String PROPERTY_CONCEPT = "property";
    public static final String ENTITY = "entity";
    private Cache<String, Concept> conceptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Inject
    public OntologyRepository() {
    }

    public List<Relationship> getRelationshipLabels(User user) {
        Iterable<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), RELATIONSHIP_CONCEPT)
                .vertices();
        return toRelationships(vertices, user);
    }

    public String getDisplayNameForLabel(String relationshipLabel, User user) {
        Iterable<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), RELATIONSHIP_CONCEPT)
                .vertices();
        for (Vertex vertex : vertices) {
            if (vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0).equals(relationshipLabel)) {
                return "" + vertex.getPropertyValue(PropertyName.DISPLAY_NAME.toString(), 0);
            }
        }
        return null;
    }

    public List<Property> getProperties(User user) {
        List<Property> properties = new ArrayList<Property>();
        Iterable<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), PROPERTY_CONCEPT)
                .vertices();
        for (Vertex vertex : vertices) {
            properties.add(new Property(vertex));
        }
        return properties;
    }

    public Property getProperty(String propertyName, User user) {
        Iterator<Vertex> properties = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), PROPERTY_CONCEPT)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (properties.hasNext()) {
            Property property = new Property(properties.next());
            if (properties.hasNext()) {
                throw new RuntimeException("Too many \"" + propertyName + "\" properties");
            }
            return property;
        } else {
            return null;
        }
    }

    public Relationship getRelationship(String propertyName, User user) {
        Iterator<Vertex> relationshipVertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), PROPERTY_CONCEPT)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (relationshipVertices.hasNext()) {
            Vertex vertex = relationshipVertices.next();
            Concept from = getConceptById(vertex.getVertices(Direction.IN, AUTHORIZATIONS).iterator().next().getId(), user);
            Concept to = getConceptById(vertex.getVertices(Direction.OUT, AUTHORIZATIONS).iterator().next().getId(), user);
            Relationship property = new Relationship(vertex, from, to);
            if (relationshipVertices.hasNext()) {
                throw new RuntimeException("Too many \"" + propertyName + "\" relationshipVertices");
            }
            return property;
        } else {
            return null;
        }
    }

    public Concept getRootConcept(User user) {
        Iterator<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.CONCEPT_TYPE.toString(), CONCEPT)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), OntologyRepository.ROOT_CONCEPT_NAME)
                .vertices()
                .iterator();
        if (vertices.hasNext()) {
            Concept concept = new Concept(vertices.next());
            if (vertices.hasNext()) {
                throw new RuntimeException("Too many \"" + OntologyRepository.ROOT_CONCEPT_NAME + "\" concepts");
            }
            return concept;
        } else {
            throw new RuntimeException("Could not find \"" + OntologyRepository.ROOT_CONCEPT_NAME + "\" concept");
        }
    }

    public List<Concept> getChildConcepts(Concept concept, User user) {
        Vertex conceptVertex = graph.getVertex(concept.getId(), AUTHORIZATIONS);
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), AUTHORIZATIONS));
    }

    public List<String> getAllSubChildrenConceptsIds(Object conceptId, List<String> conceptIds, User user) {
        Concept concept = getConceptById(conceptId, user);
        Iterable<Vertex> children = concept.getVertex().getVertices(Direction.IN, LabelName.IS_A.toString(), AUTHORIZATIONS);
        if (children != null) {
            for (Vertex child : children) {
                conceptIds.add(child.getId().toString());
                getAllSubChildrenConceptsIds(child, conceptIds, user);
            }
        }
        return conceptIds;
    }

    public Concept getParentConcept(String conceptId, User user) {
        Vertex conceptVertex = graph.getVertex(conceptId, AUTHORIZATIONS);
        Vertex parentConceptVertex = getParentConceptVertex(conceptVertex, user);
        if (parentConceptVertex == null) {
            return null;
        }
        return new Concept(parentConceptVertex);
    }

    private List<Concept> toConcepts(Iterable<Vertex> vertices) {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        for (Vertex vertex : vertices) {
            concepts.add(new Concept(vertex));
        }
        return concepts;
    }

    public Concept getConceptById(Object conceptVertexId, User user) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, AUTHORIZATIONS);
        if (conceptVertex == null) {
            return null;
        }
        return new Concept(conceptVertex);
    }

    public Concept getConceptByName(String title, User user) {
        Concept concept = conceptsCache.getIfPresent(title);
        if (concept != null) {
            return concept;
        }

        Vertex vertex = findOntologyConceptByTitle(title, user);
        if (vertex == null) {
            return null;
        }
        concept = new Concept(vertex);
        conceptsCache.put(title, concept);
        return concept;
    }

    public Vertex getGraphVertexByTitle(String title, User user) {
        return findVertexByOntologyTitle(title, user);
    }

    public List<Relationship> getRelationships(String sourceConceptTypeId, String destConceptTypeId, User user) {
        Concept sourceConcept = getConceptById(sourceConceptTypeId, user);
        if (sourceConcept == null) {
            sourceConcept = getConceptByName(sourceConceptTypeId, user);
            if (sourceConcept == null) {
                throw new RuntimeException("Could not find concept: " + sourceConceptTypeId);
            }
        }
        Concept destConcept = getConceptById(destConceptTypeId, user);
        if (destConcept == null) {
            destConcept = getConceptByName(destConceptTypeId, user);
            if (destConcept == null) {
                throw new RuntimeException("Could not find concept: " + destConceptTypeId);
            }
        }

        List<Vertex> relationshipTypes = getRelationships(sourceConcept, destConcept, user);
        return toRelationships(relationshipTypes, user);
    }

    private List<Relationship> toRelationships(Iterable<Vertex> relationshipTypes, User user) {
        ArrayList<Relationship> relationships = new ArrayList<Relationship>();
        for (Vertex vertex : relationshipTypes) {
            Concept[] relatedConcepts = getRelationshipRelatedConcepts("" + vertex.getId(), (String) vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0), user);
            relationships.add(new Relationship(vertex, relatedConcepts[0], relatedConcepts[1]));
        }
        return relationships;
    }

    private Concept[] getRelationshipRelatedConcepts(String vertexId, String ontologyTitle, User user) {
        Concept[] sourceAndDestConcept = new Concept[2];
        Iterable<Edge> edges = graph.getVertex(vertexId, AUTHORIZATIONS).getEdges(Direction.BOTH, AUTHORIZATIONS);
        for (Edge edge : edges) {
            Vertex otherVertex = edge.getOtherVertex(vertexId, AUTHORIZATIONS);
            String type = (String) otherVertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0);
            if (type.equals(CONCEPT)) {
                Object destVertexId = edge.getVertexId(Direction.IN);
                Object sourceVertexId = edge.getVertexId(Direction.OUT);
                if (sourceVertexId.equals(vertexId)) {
                    if (sourceAndDestConcept[0] != null) {
                        throw new RuntimeException("Invalid relationship '" + ontologyTitle + "'. Wrong number of related concepts.");
                    }
                    sourceAndDestConcept[0] = new Concept(otherVertex);
                } else if (destVertexId.equals(vertexId)) {
                    if (sourceAndDestConcept[1] != null) {
                        throw new RuntimeException("Invalid relationship '" + ontologyTitle + "'. Wrong number of related concepts.");
                    }
                    sourceAndDestConcept[1] = new Concept(otherVertex);
                }
            }
        }
        if (sourceAndDestConcept[0] == null || sourceAndDestConcept[1] == null) {
            throw new RuntimeException("Invalid relationship '" + ontologyTitle + "'. Wrong number of related concepts.");
        }
        return sourceAndDestConcept;
    }

    public List<Property> getPropertiesByConceptId(String conceptVertexId, User user) {
        Concept conceptVertex = getConceptById(conceptVertexId, user);
        if (conceptVertex == null) {
            conceptVertex = getConceptByName(conceptVertexId, user);
            if (conceptVertex == null) {
                throw new RuntimeException("Could not find concept: " + conceptVertexId);
            }
        }
        return getPropertiesByVertex(conceptVertex.getVertex(), user);
    }

    private List<Property> getPropertiesByVertex(Vertex vertex, User user) {
        List<Property> properties = new ArrayList<Property>();

        Iterator<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), AUTHORIZATIONS).iterator();
        while (propertyVertices.hasNext()) {
            Vertex propertyVertex = propertyVertices.next();
            properties.add(new Property(propertyVertex));
        }

        Vertex parentConceptVertex = getParentConceptVertex(vertex, user);
        if (parentConceptVertex != null) {
            List<Property> parentProperties = getPropertiesByVertex(parentConceptVertex, user);
            properties.addAll(parentProperties);
        }

        return properties;
    }

    public List<Property> getPropertiesByConceptIdNoRecursion(String conceptVertexId, User user) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, AUTHORIZATIONS);
        if (conceptVertex == null) {
            throw new RuntimeException("Could not find concept: " + conceptVertexId);
        }
        return getPropertiesByVertexNoRecursion(conceptVertex);
    }

    private List<Property> getPropertiesByVertexNoRecursion(Vertex vertex) {
        List<Property> properties = new ArrayList<Property>();

        Iterator<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), AUTHORIZATIONS).iterator();
        while (propertyVertices.hasNext()) {
            Vertex propertyVertex = propertyVertices.next();
            properties.add(new Property(propertyVertex));
        }

        return properties;
    }

    public Property getPropertyById(String propertyId, User user) {
        List<Property> properties = getProperties(user);
        for (Property property : properties) {
            if (property.getId().equals(propertyId)) {
                return property;
            }
        }
        return null;
    }

    public List<Concept> getConceptByIdAndChildren(String conceptId, User user) {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        Concept concept = getConceptById(conceptId, user);
        if (concept == null) {
            return null;
        }
        concepts.add(concept);
        List<Concept> children = getChildConcepts(concept, user);
        concepts.addAll(children);
        return concepts;
    }

    public List<Property> getPropertiesByRelationship(String relationshipLabel, User user) {
        Vertex relationshipVertex = getRelationshipVertexId(relationshipLabel, user);
        if (relationshipVertex == null) {
            throw new RuntimeException("Could not find relationship: " + relationshipLabel);
        }
        return getPropertiesByVertex(relationshipVertex, user);
    }

    private Vertex getRelationshipVertexId(String relationshipLabel, User user) {
        Iterator<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), RELATIONSHIP_CONCEPT)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), relationshipLabel)
                .vertices()
                .iterator();
        if (vertices.hasNext()) {
            Vertex vertex = vertices.next();
            if (vertices.hasNext()) {
                throw new RuntimeException("Too many \"" + RELATIONSHIP_CONCEPT + "\" vertices");
            }
            return vertex;
        } else {
            throw new RuntimeException("Could not find \"" + RELATIONSHIP_CONCEPT + "\" vertex");
        }
    }

    public Concept getOrCreateConcept(Concept parent, String conceptName, String displayName, User user) {
        Concept concept = getConceptByName(conceptName, user);
        if (concept == null) {
            Vertex vertex = graph.addVertex(DEFAULT_VISIBILITY);
            concept = new Concept(vertex);
        }
        graph = concept.getVertex().getGraph();
        concept.getVertex().setProperties(
                graph.createProperty(PropertyName.CONCEPT_TYPE.toString(), CONCEPT, DEFAULT_VISIBILITY),
                graph.createProperty(PropertyName.ONTOLOGY_TITLE.toString(), conceptName, DEFAULT_VISIBILITY),
                graph.createProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY)
        );
        if (parent != null) {
            findOrAddEdge(concept.getVertex(), parent.getVertex(), LabelName.IS_A.toString(), user);
        }

        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel, User user) {
        Iterator<Vertex> matchingEdges = new FilterIterable<Vertex>(fromVertex.getVertices(Direction.BOTH, edgeLabel, AUTHORIZATIONS)) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        }.iterator();
        if (!matchingEdges.hasNext()) {
            fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, DEFAULT_VISIBILITY);
        }
    }

    public Property addPropertyTo(Vertex vertex, String propertyName, String displayName, PropertyType dataType, User user) {
        checkNotNull(vertex, "vertex was null");
        Property property = getOrCreatePropertyType(propertyName, dataType, user);
        property.getVertex().setProperties(
                property.getVertex().getGraph().createProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY)
        );

        findOrAddEdge(vertex, property.getVertex(), LabelName.HAS_PROPERTY.toString(), user);

        return property;
    }

    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipName, String displayName, User user) {
        Relationship relationship = getOrCreateRelationshipType(relationshipName, from, to, user);
        relationship.getVertex().setProperties(graph.createProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY));

        findOrAddEdge(from.getVertex(), relationship.getVertex(), LabelName.HAS_EDGE.toString(), user);
        findOrAddEdge(relationship.getVertex(), to.getVertex(), LabelName.HAS_EDGE.toString(), user);

        return relationship;
    }

    public void resolvePropertyIds(JSONArray filterJson, User user) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyId = filter.getString("propertyId");
                Property property = getPropertyById(propertyId, user);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    public Map<Object, Concept> getAllConceptsById(User user) {
        Map<Object, Concept> results = new HashMap<Object, Concept>();
        Concept rootConcept = getRootConcept(user);
        results.put(rootConcept.getId(), rootConcept);
        getAllConceptsById(results, rootConcept, user);
        return results;
    }

    private void getAllConceptsById(Map<Object, Concept> concepts, Concept rootConcept, User user) {
        List<Concept> childConcepts = getChildConcepts(rootConcept, user);
        for (Concept c : childConcepts) {
            concepts.put(c.getId(), c);
            getAllConceptsById(concepts, c, user);
        }
    }

    public Map<String, Concept> getAllConceptsByTitle(User user) {
        Map<String, Concept> results = new HashMap<String, Concept>();
        Concept rootConcept = getRootConcept(user);
        results.put(rootConcept.getTitle(), rootConcept);
        getAllConceptsByTitle(results, rootConcept, user);
        return results;
    }

    private void getAllConceptsByTitle(Map<String, Concept> concepts, Concept rootConcept, User user) {
        List<Concept> childConcepts = getChildConcepts(rootConcept, user);
        for (Concept c : childConcepts) {
            concepts.put(c.getTitle(), c);
            getAllConceptsByTitle(concepts, c, user);
        }
    }

    public Property getOrCreatePropertyType(String name, PropertyType dataType, User user) {
        Property typeProperty = getProperty(name, user);
        if (typeProperty == null) {
            typeProperty = new Property(graph.addVertex(DEFAULT_VISIBILITY));
        }
        typeProperty.getVertex().setProperties(
                graph.createProperty(PropertyName.DISPLAY_TYPE.toString(), OntologyRepository.PROPERTY_CONCEPT.toString(), DEFAULT_VISIBILITY),
                graph.createProperty(PropertyName.ONTOLOGY_TITLE.toString(), name, DEFAULT_VISIBILITY),
                graph.createProperty(PropertyName.DATA_TYPE.toString(), dataType.toString(), DEFAULT_VISIBILITY)
        );
        return typeProperty;
    }

    private Relationship getOrCreateRelationshipType(String relationshipName, Concept from, Concept to, User user) {
        Relationship relationship = getRelationship(relationshipName, user);
        if (relationship == null) {
            relationship = new Relationship(graph.addVertex(DEFAULT_VISIBILITY), from, to);
        }
        relationship.getVertex().setProperties(
                graph.createProperty(PropertyName.DISPLAY_TYPE.toString(), OntologyRepository.RELATIONSHIP_CONCEPT.toString(), DEFAULT_VISIBILITY),
                graph.createProperty(PropertyName.ONTOLOGY_TITLE.toString(), relationshipName, DEFAULT_VISIBILITY)
        );
        return relationship;
    }

    private List<Vertex> getRelationships(Concept sourceConcept, final Concept destConcept, User user) {
        List<Vertex> sourceAndParents = getConceptParents(sourceConcept, user);
        List<Vertex> destAndParents = getConceptParents(destConcept, user);

        List<Vertex> allRelationshipTypes = new ArrayList<Vertex>();
        for (Vertex s : sourceAndParents) {
            for (Vertex d : destAndParents) {
                allRelationshipTypes.addAll(getRelationshipsShallow(s, d));
            }
        }

        return allRelationshipTypes;
    }

    private List<Vertex> getRelationshipsShallow(Vertex source, final Vertex dest) {
        return new GremlinPipeline(source)
                .outE(LabelName.HAS_EDGE.toString())
                .inV()
                .as("edgeTypes")
                .outE(LabelName.HAS_EDGE.toString())
                .inV()
                .filter(new PipeFunction<Vertex, Boolean>() {
                    @Override
                    public Boolean compute(Vertex vertex) {
                        return vertex.getId().equals(dest.getId());
                    }
                })
                .back("edgeTypes")
                .toList();
    }

    private List<Vertex> getConceptParents(Concept concept, User user) {
        ArrayList<Vertex> results = new ArrayList<Vertex>();
        results.add(concept.getVertex());
        Vertex v = concept.getVertex();
        while ((v = getParentConceptVertex(v, user)) != null) {
            results.add(v);
        }
        return results;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex, User user) {
        Iterator<Vertex> parents = conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), AUTHORIZATIONS).iterator();
        if (!parents.hasNext()) {
            return null;
        }
        Vertex v = parents.next();
        if (parents.hasNext()) {
            throw new RuntimeException("Unexpected number of parents for concept: " + conceptVertex.getPropertyValue(PropertyName.TITLE.toString(), 0));
        }
        return v;
    }

    private Vertex findOntologyConceptByTitle(String title, User user) {
        Iterator<Vertex> r = graph.query(AUTHORIZATIONS)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .has(PropertyName.CONCEPT_TYPE.toString(), OntologyRepository.CONCEPT.toString())
                .vertices()
                .iterator();
        if (r.hasNext()) {
            return r.next();
        }
        return null;
    }

    private Vertex findVertexByOntologyTitle(String title, User user) {
        Iterator<Vertex> r = graph.query(AUTHORIZATIONS)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .vertices()
                .iterator();
        if (r.hasNext()) {
            return r.next();
        }
        return null;
    }
}
