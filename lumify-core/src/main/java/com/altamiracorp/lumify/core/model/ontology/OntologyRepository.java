package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.graph.InMemoryGraphVertex;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.*;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.thinkaurelius.titan.core.TitanKey;
import com.thinkaurelius.titan.core.TitanType;
import com.thinkaurelius.titan.core.TypeMaker;
import com.thinkaurelius.titan.core.attribute.Geoshape;
import com.tinkerpop.gremlin.java.GremlinPipeline;
import com.tinkerpop.pipes.PipeFunction;
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
        Iterator<Vertex> vertices = graph.query(AUTHORIZATIONS)
                .has(PropertyName.DISPLAY_TYPE.toString(), PROPERTY_CONCEPT)
                .vertices()
                .iterator();
        while (vertices.hasNext()) {
            Vertex vertex = vertices.next();
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

    public Concept getConceptById(String conceptVertexId, User user) {
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
            InMemoryGraphVertex graphVertex = new InMemoryGraphVertex();
            String id = graphRepository.saveVertex(graphVertex, user);
            concept = getConceptById(id, user);
        }
        concept.setProperty(PropertyName.CONCEPT_TYPE.toString(), CONCEPT);
        concept.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), conceptName);
        concept.setProperty(PropertyName.DISPLAY_NAME.toString(), displayName);
        if (parent != null) {
            graphRepository.findOrAddRelationship(concept, parent, LabelName.IS_A, user);
        }

        graphSession.commit();

        return concept;
    }

    protected void findOrAddEdge(GraphVertex fromVertex, GraphVertex toVertex, String edgeLabel, User user) {
        graphSession.findOrAddEdge(fromVertex, toVertex, edgeLabel, user);
    }

    public Property addPropertyTo(Vertex vertex, String propertyName, String displayName, PropertyType dataType, User user) {
        checkNotNull(vertex, "vertex was null");
        Property property = graphSession.getOrCreatePropertyType(propertyName, dataType, user);
        property.setProperty(PropertyName.DISPLAY_NAME.toString(), displayName);
        graphSession.commit();

        findOrAddEdge(vertex, property, LabelName.HAS_PROPERTY.toString(), user);
        graphSession.commit();

        return property;
    }

    public GraphVertex getOrCreateRelationshipType(Vertex fromVertex, Vertex toVertex, String relationshipName, String displayName, User user) {
        GraphVertex relationshipLabel = graphSession.getOrCreateRelationshipType(relationshipName, user);
        relationshipLabel.setProperty(PropertyName.DISPLAY_NAME.toString(), displayName);
        graphSession.commit();

        findOrAddEdge(fromVertex, relationshipLabel, LabelName.HAS_EDGE.toString(), user);
        findOrAddEdge(relationshipLabel, toVertex, LabelName.HAS_EDGE.toString(), user);
        graphSession.commit();

        return relationshipLabel;
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

    public Map<String, Concept> getAllConceptsById(User user) {
        Map<String, Concept> results = new HashMap<String, Concept>();
        Concept rootConcept = getRootConcept(user);
        results.put(rootConcept.getId(), rootConcept);
        getAllConceptsById(results, rootConcept, user);
        return results;
    }

    private void getAllConceptsById(Map<String, Concept> concepts, Concept rootConcept, User user) {
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
        TitanKey typeProperty = (TitanKey) graph.getType(name);
        Property v;
        if (typeProperty != null) {
            v = new Property(typeProperty);
        } else {
            Class vertexDataType = String.class;
            switch (dataType) {
                case DATE:
                    vertexDataType = Long.class;
                    break;
                case DOUBLE:
                case CURRENCY:
                    vertexDataType = Double.class;
                    break;
                case IMAGE:
                case STRING:
                    vertexDataType = String.class;
                    break;
                case GEO_LOCATION:
                    vertexDataType = Geoshape.class;
                    break;
                default:
                    LOGGER.error("Unknown PropertyType [%s] for Property [%s].  Using String type.", dataType, name);
                    vertexDataType = String.class;
                    break;
            }
            v = new Property(graph.makeType().name(name).dataType(vertexDataType).unique(Direction.OUT, TypeMaker.UniquenessConsistency.NO_LOCK).indexed(com.tinkerpop.blueprints.Vertex.class).makePropertyKey());
        }
        v.setProperty(PropertyName.DISPLAY_TYPE.toString(), OntologyRepository.PROPERTY_CONCEPT.toString());
        v.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), name);
        v.setProperty(PropertyName.DATA_TYPE.toString(), dataType.toString());
        return v;
    }

    private GraphVertex getOrCreateRelationshipType(String relationshipName, User user) {
        TitanType relationshipLabel = graph.getType(relationshipName);
        TitanGraphVertex v;
        if (relationshipLabel != null) {
            v = new TitanGraphVertex(relationshipLabel);
        } else {
            v = new TitanGraphVertex(graph.makeType().name(relationshipName).directed().makeEdgeLabel());
        }
        v.setProperty(PropertyName.DISPLAY_TYPE.toString(), OntologyRepository.RELATIONSHIP_CONCEPT.toString());
        v.setProperty(PropertyName.ONTOLOGY_TITLE.toString(), relationshipName);
        return v;
    }

    private List<Vertex> getRelationships(Concept sourceConcept, final Concept destConcept, User user) {
        List<com.tinkerpop.blueprints.Vertex> sourceAndParents = getConceptParents(sourceConcept, user);
        List<com.tinkerpop.blueprints.Vertex> destAndParents = getConceptParents(destConcept, user);

        List<com.tinkerpop.blueprints.Vertex> allRelationshipTypes = new ArrayList<com.tinkerpop.blueprints.Vertex>();
        for (com.tinkerpop.blueprints.Vertex s : sourceAndParents) {
            for (com.tinkerpop.blueprints.Vertex d : destAndParents) {
                allRelationshipTypes.addAll(getRelationshipsShallow(s, d));
            }
        }

        return allRelationshipTypes;
    }

    private List<com.tinkerpop.blueprints.Vertex> getRelationshipsShallow(com.tinkerpop.blueprints.Vertex source, final com.tinkerpop.blueprints.Vertex dest) {
        return new GremlinPipeline(source)
                .outE(LabelName.HAS_EDGE.toString())
                .inV()
                .as("edgeTypes")
                .outE(LabelName.HAS_EDGE.toString())
                .inV()
                .filter(new PipeFunction<com.tinkerpop.blueprints.Vertex, Boolean>() {
                    @Override
                    public Boolean compute(com.tinkerpop.blueprints.Vertex vertex) {
                        return vertex.getId().equals(dest.getId());
                    }
                })
                .back("edgeTypes")
                .toList();
    }

    private List<com.tinkerpop.blueprints.Vertex> getConceptParents(Concept concept, User user) {
        ArrayList<com.tinkerpop.blueprints.Vertex> results = new ArrayList<com.tinkerpop.blueprints.Vertex>();
        results.add(concept.getVertex());
        com.tinkerpop.blueprints.Vertex v = concept.getVertex();
        while ((v = getParentConceptVertex(v, user)) != null) {
            results.add(v);
        }
        return results;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex, User user) {
        Iterator<com.tinkerpop.blueprints.Vertex> parents = conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString()).iterator();
        if (!parents.hasNext()) {
            return null;
        }
        com.tinkerpop.blueprints.Vertex v = parents.next();
        if (parents.hasNext()) {
            throw new RuntimeException("Unexpected number of parents for concept: " + conceptVertex.getProperty(PropertyName.TITLE.toString()));
        }
        return v;
    }

    private Vertex findOntologyConceptByTitle(String title, User user) {
        Iterable<com.tinkerpop.blueprints.Vertex> r = graph.query()
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .has(PropertyName.CONCEPT_TYPE.toString(), OntologyRepository.CONCEPT.toString())
                .vertices();
        ArrayList<GraphVertex> graphVertices = toGraphVertices(r);
        if (graphVertices.size() > 0) {
            return graphVertices.get(0);
        }
        return null;
    }

    private Vertex findVertexByOntologyTitle(String title, User user) {
        Iterable<com.tinkerpop.blueprints.Vertex> r = graph.query()
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .vertices();
        ArrayList<GraphVertex> graphVertices = toGraphVertices(r);
        if (graphVertices.size() > 0) {
            return graphVertices.get(0);
        }
        return null;
    }
}
