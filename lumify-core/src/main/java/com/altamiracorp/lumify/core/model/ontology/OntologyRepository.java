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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class OntologyRepository {
    private static final Visibility DEFAULT_VISIBILITY = new Visibility("");
    private final Graph graph;
    public static final String ROOT_CONCEPT_NAME = "rootConcept";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String TYPE_ENTITY = "entity";
    private Cache<String, Concept> conceptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Inject
    public OntologyRepository(Graph graph) {
        this.graph = graph;
    }

    public List<Relationship> getRelationshipLabels(User user) {
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
                .vertices();
        return toRelationships(vertices, user);
    }

    public String getDisplayNameForLabel(String relationshipLabel, User user) {
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
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
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_PROPERTY)
                .vertices();
        for (Vertex vertex : vertices) {
            properties.add(new Property(vertex));
        }
        return properties;
    }

    public Property getProperty(String propertyName, User user) {
        Iterator<Vertex> properties = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_PROPERTY)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (properties.hasNext()) {
            Vertex vertex = properties.next();
            Property property = new Property(vertex);
            if (properties.hasNext()) {
                throw new RuntimeException("Too many \"" + propertyName + "\" properties");
            }
            return property;
        } else {
            return null;
        }
    }

    public Relationship getRelationship(String propertyName, User user) {
        Iterator<Vertex> relationshipVertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (relationshipVertices.hasNext()) {
            Vertex vertex = relationshipVertices.next();
            Concept from = getConceptById(vertex.getVertices(Direction.IN, user.getAuthorizations()).iterator().next().getId(), user);
            Concept to = getConceptById(vertex.getVertices(Direction.OUT, user.getAuthorizations()).iterator().next().getId(), user);
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
        Iterator<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.CONCEPT_TYPE.toString(), TYPE_CONCEPT)
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
        Vertex conceptVertex = graph.getVertex(concept.getId(), user.getAuthorizations());
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), user.getAuthorizations()));
    }

    public Concept getParentConcept(String conceptId, User user) {
        Vertex conceptVertex = graph.getVertex(conceptId, user.getAuthorizations());
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
        Vertex conceptVertex = graph.getVertex(conceptVertexId, user.getAuthorizations());
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

        return getRelationships(sourceConcept, destConcept, user);
    }

    private List<Relationship> toRelationships(Iterable<Vertex> relationshipTypes, User user) {
        ArrayList<Relationship> relationships = new ArrayList<Relationship>();
        for (Vertex vertex : relationshipTypes) {
            Concept[] relatedConcepts = getRelationshipRelatedConcepts(vertex.getId(), (String) vertex.getPropertyValue(PropertyName.ONTOLOGY_TITLE.toString(), 0), user);
            relationships.add(new Relationship(vertex, relatedConcepts[0], relatedConcepts[1]));
        }
        return relationships;
    }

    private Concept[] getRelationshipRelatedConcepts(Object vertexId, String ontologyTitle, User user) {
        Concept[] sourceAndDestConcept = new Concept[2];
        Iterable<Edge> edges = graph.getVertex(vertexId, user.getAuthorizations()).getEdges(Direction.BOTH, user.getAuthorizations());
        for (Edge edge : edges) {
            Vertex otherVertex = edge.getOtherVertex(vertexId, user.getAuthorizations());
            String type = (String) otherVertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0);
            if (type.equals(TYPE_CONCEPT)) {
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

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), user.getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
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
        Vertex conceptVertex = graph.getVertex(conceptVertexId, user.getAuthorizations());
        if (conceptVertex == null) {
            throw new RuntimeException("Could not find concept: " + conceptVertexId);
        }
        return getPropertiesByVertexNoRecursion(conceptVertex, user);
    }

    private List<Property> getPropertiesByVertexNoRecursion(Vertex vertex, User user) {
        List<Property> properties = new ArrayList<Property>();

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), user.getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
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
        Iterator<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), relationshipLabel)
                .vertices()
                .iterator();
        if (vertices.hasNext()) {
            Vertex vertex = vertices.next();
            if (vertices.hasNext()) {
                throw new RuntimeException("Too many \"" + TYPE_RELATIONSHIP + "\" vertices");
            }
            return vertex;
        } else {
            throw new RuntimeException("Could not find \"" + TYPE_RELATIONSHIP + "\" vertex");
        }
    }

    public Concept getOrCreateConcept(Concept parent, String conceptName, String displayName, User user) {
        Concept concept = getConceptByName(conceptName, user);
        if (concept != null) {
            return concept;
        }

        Vertex vertex = graph.prepareVertex(DEFAULT_VISIBILITY)
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), TYPE_CONCEPT, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), conceptName, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY)
                .save();
        concept = new Concept(vertex);
        if (parent != null) {
            findOrAddEdge(concept.getVertex(), parent.getVertex(), LabelName.IS_A.toString(), user);
        }

        graph.flush();
        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel, User user) {
        Iterator<Vertex> matchingEdges = new FilterIterable<Vertex>(fromVertex.getVertices(Direction.BOTH, edgeLabel, user.getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        }.iterator();
        if (matchingEdges.hasNext()) {
            return;
        }
        fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, DEFAULT_VISIBILITY);
    }

    public Property addPropertyTo(Vertex vertex, String propertyName, String displayName, PropertyType dataType, User user) {
        checkNotNull(vertex, "vertex was null");
        Property property = getOrCreatePropertyType(propertyName, dataType, user);
        checkNotNull(property, "Could not find property: " + propertyName);
        property.getVertex().setProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY);

        findOrAddEdge(vertex, property.getVertex(), LabelName.HAS_PROPERTY.toString(), user);

        graph.flush();
        return property;
    }

    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipName, String displayName, User user) {
        Relationship relationship = getRelationship(relationshipName, user);
        if (relationship != null) {
            return relationship;
        }

        Vertex relationshipVertex = graph.prepareVertex(DEFAULT_VISIBILITY)
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), TYPE_CONCEPT, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), relationshipName, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY)
                .save();

        findOrAddEdge(from.getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString(), user);
        findOrAddEdge(relationshipVertex, to.getVertex(), LabelName.HAS_EDGE.toString(), user);

        graph.flush();
        return new Relationship(relationshipVertex, from, to);
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

    public Property getOrCreatePropertyType(String name, PropertyType dataType, User user) {
        Property typeProperty = getProperty(name, user);
        if (typeProperty != null) {
            return typeProperty;
        }

        typeProperty = new Property(graph.prepareVertex(DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_TYPE.toString(), OntologyRepository.TYPE_PROPERTY, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), name, DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DATA_TYPE.toString(), dataType.toString(), DEFAULT_VISIBILITY)
                .save());

        graph.flush();
        return typeProperty;
    }

    private List<Relationship> getRelationships(Concept sourceConcept, final Concept destConcept, User user) {
        List<Vertex> sourceAndParents = getConceptParents(sourceConcept, user);
        List<Vertex> destAndParents = getConceptParents(destConcept, user);

        List<Relationship> allRelationshipTypes = new ArrayList<Relationship>();
        for (Vertex s : sourceAndParents) {
            for (Vertex d : destAndParents) {
                allRelationshipTypes.addAll(getRelationshipsShallow(s, d, user));
            }
        }

        return allRelationshipTypes;
    }

    private List<Relationship> getRelationshipsShallow(Vertex source, final Vertex dest, final User user) {
        FilterIterable<Vertex> relationships = new FilterIterable<Vertex>(source.getVertices(Direction.BOTH, LabelName.HAS_EDGE.toString(), user.getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return new FilterIterable<Vertex>(vertex.getVertices(Direction.BOTH, LabelName.HAS_EDGE.toString(), user.getAuthorizations())) {
                    @Override
                    protected boolean isIncluded(Vertex vertex) {
                        return vertex.getId().equals(dest.getId());
                    }
                }.iterator().hasNext();
            }
        };
        return toRelationships(relationships, user);
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
        Iterator<Vertex> parents = conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), user.getAuthorizations()).iterator();
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
        Iterator<Vertex> r = graph.query(user.getAuthorizations())
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .has(PropertyName.CONCEPT_TYPE.toString(), OntologyRepository.TYPE_CONCEPT)
                .vertices()
                .iterator();
        if (r.hasNext()) {
            return r.next();
        }
        return null;
    }

    private Vertex findVertexByOntologyTitle(String title, User user) {
        Iterator<Vertex> r = graph.query(user.getAuthorizations())
                .has(PropertyName.ONTOLOGY_TITLE.toString(), title)
                .vertices()
                .iterator();
        if (r.hasNext()) {
            return r.next();
        }
        return null;
    }
}
