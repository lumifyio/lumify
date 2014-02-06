package com.altamiracorp.lumify.core.model.ontology;

import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.securegraph.*;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.altamiracorp.securegraph.util.FilterIterable;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.altamiracorp.lumify.core.util.CollectionUtil.single;
import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class OntologyRepository {
    public static final String VISIBILITY_STRING = "ontology";
    public static final Visibility DEFAULT_VISIBILITY = new Visibility(VISIBILITY_STRING);
    private final Graph graph;
    public static final String ROOT_CONCEPT_NAME = "rootConcept";
    public static final String TYPE_RELATIONSHIP = "relationship";
    public static final String TYPE_CONCEPT = "concept";
    public static final String TYPE_PROPERTY = "property";
    public static final String TYPE_ENTITY = "entity";
    private User user;
    private Cache<String, Concept> conceptsCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    @Inject
    public OntologyRepository(Graph graph, UserProvider userProvider) {
        this.graph = graph;
        this.user = userProvider.getOntologyUser();
    }

    public Iterable<Relationship> getRelationshipLabels() {
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
                .vertices();

        return new ConvertingIterable<Vertex, Relationship>(vertices) {
            @Override
            protected Relationship convert(Vertex vertex) {
                Vertex sourceVertex = single(vertex.getVertices(Direction.IN, user.getAuthorizations()));
                Vertex destVertex = single(vertex.getVertices(Direction.OUT, user.getAuthorizations()));
                return new Relationship(vertex, new Concept(sourceVertex), new Concept(destVertex));
            }
        };
    }

    public Iterable<Relationship> getRelationships(String sourceConceptTypeId, String destConceptTypeId) {
        Concept sourceConcept = getConceptById(sourceConceptTypeId);
        if (sourceConcept == null) {
            sourceConcept = getConceptByName(sourceConceptTypeId);
            if (sourceConcept == null) {
                throw new RuntimeException("Could not find concept: " + sourceConceptTypeId);
            }
        }
        Concept destConcept = getConceptById(destConceptTypeId);
        if (destConcept == null) {
            destConcept = getConceptByName(destConceptTypeId);
            if (destConcept == null) {
                throw new RuntimeException("Could not find concept: " + destConceptTypeId);
            }
        }

        return getRelationships(sourceConcept, destConcept);
    }

    private Iterable<Relationship> getRelationships(final Concept sourceConcept, final Concept destConcept) {
        final List<Vertex> sourceAndParents = getConceptParents(sourceConcept);
        final List<Vertex> destAndParents = getConceptParents(destConcept);

        return new FilterIterable<Relationship>(getRelationshipLabels()) {
            @Override
            protected boolean isIncluded(Relationship relationship) {
                Object sourceId = relationship.getSourceConcept().getId();
                Object destId = relationship.getDestConcept().getId();
                for (Vertex s : sourceAndParents) {
                    if (!s.getId().equals(sourceId)) {
                        continue;
                    }
                    for (Vertex d : destAndParents) {
                        if (d.getId().equals(destId)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    public String getDisplayNameForLabel(String relationshipLabel) {
        Concept relationship = getConceptByName(relationshipLabel);
        Vertex vertex = relationship.getVertex();
        if (vertex != null) {
            return "" + vertex.getPropertyValue(PropertyName.DISPLAY_NAME.toString(), 0);
        }
        return null;
    }

    public List<OntologyProperty> getProperties() {
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();
        Iterable<Vertex> vertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_PROPERTY)
                .vertices();
        for (Vertex vertex : vertices) {
            properties.add(new OntologyProperty(vertex));
        }
        return properties;
    }

    public OntologyProperty getProperty(String propertyName) {
        Iterator<Vertex> properties = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_PROPERTY)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (properties.hasNext()) {
            Vertex vertex = properties.next();
            OntologyProperty property = new OntologyProperty(vertex);
            if (properties.hasNext()) {
                throw new RuntimeException("Too many \"" + propertyName + "\" properties");
            }
            return property;
        } else {
            return null;
        }
    }

    public Relationship getRelationship(String propertyName) {
        Iterator<Vertex> relationshipVertices = graph.query(user.getAuthorizations())
                .has(PropertyName.DISPLAY_TYPE.toString(), TYPE_RELATIONSHIP)
                .has(PropertyName.ONTOLOGY_TITLE.toString(), propertyName)
                .vertices()
                .iterator();
        if (relationshipVertices.hasNext()) {
            Vertex vertex = relationshipVertices.next();
            Concept from = getConceptById(vertex.getVertices(Direction.IN, user.getAuthorizations()).iterator().next().getId());
            Concept to = getConceptById(vertex.getVertices(Direction.OUT, user.getAuthorizations()).iterator().next().getId());
            Relationship property = new Relationship(vertex, from, to);
            if (relationshipVertices.hasNext()) {
                throw new RuntimeException("Too many \"" + propertyName + "\" relationshipVertices");
            }
            return property;
        } else {
            return null;
        }
    }

    public Concept getRootConcept() {
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

    public List<Concept> getChildConcepts(Concept concept) {
        Vertex conceptVertex = graph.getVertex(concept.getId(), user.getAuthorizations());
        return toConcepts(conceptVertex.getVertices(Direction.IN, LabelName.IS_A.toString(), user.getAuthorizations()));
    }

    public Concept getParentConcept(final Concept concept) {
        return getParentConcept(concept.getId().toString());
    }

    public Concept getParentConcept(String conceptId) {
        Vertex conceptVertex = graph.getVertex(conceptId, user.getAuthorizations());
        Vertex parentConceptVertex = getParentConceptVertex(conceptVertex);
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

    public Concept getConceptById(Object conceptVertexId) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, user.getAuthorizations());
        if (conceptVertex == null) {
            return null;
        }
        return new Concept(conceptVertex);
    }

    public Concept getConceptByName(String title) {
        Concept concept = conceptsCache.getIfPresent(title);
        if (concept != null) {
            return concept;
        }

        Vertex vertex = findOntologyConceptByTitle(title);
        if (vertex == null) {
            return null;
        }
        concept = new Concept(vertex);
        conceptsCache.put(title, concept);
        return concept;
    }

    public Vertex getGraphVertexByTitle(String title) {
        return findVertexByOntologyTitle(title);
    }

    public List<OntologyProperty> getPropertiesByConceptId(String conceptVertexId) {
        Concept conceptVertex = getConceptById(conceptVertexId);
        if (conceptVertex == null) {
            conceptVertex = getConceptByName(conceptVertexId);
            if (conceptVertex == null) {
                throw new RuntimeException("Could not find concept: " + conceptVertexId);
            }
        }
        return getPropertiesByVertex(conceptVertex.getVertex());
    }

    private List<OntologyProperty> getPropertiesByVertex(Vertex vertex) {
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), user.getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
            properties.add(new OntologyProperty(propertyVertex));
        }

        Vertex parentConceptVertex = getParentConceptVertex(vertex);
        if (parentConceptVertex != null) {
            List<OntologyProperty> parentProperties = getPropertiesByVertex(parentConceptVertex);
            properties.addAll(parentProperties);
        }

        return properties;
    }

    public List<OntologyProperty> getPropertiesByConceptIdNoRecursion(String conceptVertexId) {
        Vertex conceptVertex = graph.getVertex(conceptVertexId, user.getAuthorizations());
        if (conceptVertex == null) {
            throw new RuntimeException("Could not find concept: " + conceptVertexId);
        }
        return getPropertiesByVertexNoRecursion(conceptVertex);
    }

    private List<OntologyProperty> getPropertiesByVertexNoRecursion(Vertex vertex) {
        List<OntologyProperty> properties = new ArrayList<OntologyProperty>();

        Iterable<Vertex> propertyVertices = vertex.getVertices(Direction.OUT, LabelName.HAS_PROPERTY.toString(), user.getAuthorizations());
        for (Vertex propertyVertex : propertyVertices) {
            properties.add(new OntologyProperty(propertyVertex));
        }

        return properties;
    }

    public OntologyProperty getPropertyById(String propertyId) {
        List<OntologyProperty> properties = getProperties();
        for (OntologyProperty property : properties) {
            if (property.getId().equals(propertyId)) {
                return property;
            }
        }
        return null;
    }

    public List<Concept> getConceptByIdAndChildren(String conceptId) {
        ArrayList<Concept> concepts = new ArrayList<Concept>();
        Concept concept = getConceptById(conceptId);
        if (concept == null) {
            return null;
        }
        concepts.add(concept);
        List<Concept> children = getChildConcepts(concept);
        concepts.addAll(children);
        return concepts;
    }

    public List<Concept> getAllLeafNodesByConcept(Concept concept) {
        List<Concept> childConcepts = getChildConcepts(concept);
        List<Concept> parent = Lists.newArrayList(concept);
        if (childConcepts.size() > 0) {
            List<Concept> childrenList = new ArrayList<Concept>();
            for (Concept childConcept : childConcepts) {
                List<Concept> child = getAllLeafNodesByConcept(childConcept);
                childrenList.addAll(child);
            }
            parent.addAll(childrenList);
        }
        return parent;
    }

    public List<OntologyProperty> getPropertiesByRelationship(String relationshipLabel) {
        Vertex relationshipVertex = getRelationshipVertexId(relationshipLabel);
        if (relationshipVertex == null) {
            throw new RuntimeException("Could not find relationship: " + relationshipLabel);
        }
        return getPropertiesByVertex(relationshipVertex);
    }

    private Vertex getRelationshipVertexId(String relationshipLabel) {
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

    public Concept getOrCreateConcept(Concept parent, String conceptName, String displayName) {
        Concept concept = getConceptByName(conceptName);
        if (concept != null) {
            return concept;
        }

        Vertex vertex = graph.prepareVertex(DEFAULT_VISIBILITY, user.getAuthorizations())
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), new Text(TYPE_CONCEPT, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), new Text(conceptName, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_NAME.toString(), new Text(displayName), DEFAULT_VISIBILITY)
                .save();
        concept = new Concept(vertex);
        if (parent != null) {
            findOrAddEdge(concept.getVertex(), parent.getVertex(), LabelName.IS_A.toString());
        }

        graph.flush();
        return concept;
    }

    protected void findOrAddEdge(Vertex fromVertex, final Vertex toVertex, String edgeLabel) {
        Iterator<Vertex> matchingEdges = new FilterIterable<Vertex>(fromVertex.getVertices(Direction.BOTH, edgeLabel, user.getAuthorizations())) {
            @Override
            protected boolean isIncluded(Vertex vertex) {
                return vertex.getId().equals(toVertex.getId());
            }
        }.iterator();
        if (matchingEdges.hasNext()) {
            return;
        }
        fromVertex.getGraph().addEdge(fromVertex, toVertex, edgeLabel, DEFAULT_VISIBILITY, user.getAuthorizations());
    }

    public OntologyProperty addPropertyTo(Vertex vertex, String propertyName, String displayName, PropertyType dataType) {
        checkNotNull(vertex, "vertex was null");
        OntologyProperty property = getOrCreatePropertyType(propertyName, dataType);
        checkNotNull(property, "Could not find property: " + propertyName);
        property.getVertex().setProperty(PropertyName.DISPLAY_NAME.toString(), displayName, DEFAULT_VISIBILITY);

        findOrAddEdge(vertex, property.getVertex(), LabelName.HAS_PROPERTY.toString());

        graph.flush();
        return property;
    }

    public Relationship getOrCreateRelationshipType(Concept from, Concept to, String relationshipName, String displayName) {
        Relationship relationship = getRelationship(relationshipName);
        if (relationship != null) {
            return relationship;
        }

        Vertex relationshipVertex = graph.prepareVertex(DEFAULT_VISIBILITY, user.getAuthorizations())
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), new Text(TYPE_CONCEPT, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), new Text(relationshipName, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_NAME.toString(), new Text(displayName), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_TYPE.toString(), new Text(TYPE_RELATIONSHIP, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .save();

        findOrAddEdge(from.getVertex(), relationshipVertex, LabelName.HAS_EDGE.toString());
        findOrAddEdge(relationshipVertex, to.getVertex(), LabelName.HAS_EDGE.toString());

        graph.flush();
        return new Relationship(relationshipVertex, from, to);
    }

    public void resolvePropertyIds(JSONArray filterJson) throws JSONException {
        for (int i = 0; i < filterJson.length(); i++) {
            JSONObject filter = filterJson.getJSONObject(i);
            if (filter.has("propertyId") && !filter.has("propertyName")) {
                String propertyId = filter.getString("propertyId");
                OntologyProperty property = getPropertyById(propertyId);
                if (property == null) {
                    throw new RuntimeException("Could not find property with id: " + propertyId);
                }
                filter.put("propertyName", property.getTitle());
                filter.put("propertyDataType", property.getDataType());
            }
        }
    }

    public OntologyProperty getOrCreatePropertyType(String name, PropertyType dataType) {
        OntologyProperty typeProperty = getProperty(name);
        if (typeProperty != null) {
            return typeProperty;
        }

        typeProperty = new OntologyProperty(graph.prepareVertex(DEFAULT_VISIBILITY, user.getAuthorizations())
                .setProperty(PropertyName.CONCEPT_TYPE.toString(), new Text(TYPE_PROPERTY, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DISPLAY_TYPE.toString(), new Text(OntologyRepository.TYPE_PROPERTY, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.ONTOLOGY_TITLE.toString(), new Text(name, TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .setProperty(PropertyName.DATA_TYPE.toString(), new Text(dataType.toString(), TextIndexHint.EXACT_MATCH), DEFAULT_VISIBILITY)
                .save());

        graph.flush();
        return typeProperty;
    }

    private List<Vertex> getConceptParents(Concept concept) {
        ArrayList<Vertex> results = new ArrayList<Vertex>();
        results.add(concept.getVertex());
        Vertex v = concept.getVertex();
        while ((v = getParentConceptVertex(v)) != null) {
            results.add(v);
        }
        return results;
    }

    private Vertex getParentConceptVertex(Vertex conceptVertex) {
        Iterator<Vertex> parents = conceptVertex.getVertices(Direction.OUT, LabelName.IS_A.toString(), user.getAuthorizations()).iterator();
        if (!parents.hasNext()) {
            return null;
        }
        Vertex v = parents.next();
        if (parents.hasNext()) {
            throw new RuntimeException("Unexpected number of parents for concept: " + conceptVertex.getPropertyValue(PropertyName.TITLE.toString()));
        }
        return v;
    }

    private Vertex findOntologyConceptByTitle(String title) {
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

    private Vertex findVertexByOntologyTitle(String title) {
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
