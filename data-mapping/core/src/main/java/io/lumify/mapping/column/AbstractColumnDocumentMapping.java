package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.mapping.DocumentMapping;
import io.lumify.mapping.State;
import io.lumify.mapping.column.ColumnRelationshipMapping.RelationshipDef;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.securegraph.Authorizations;
import org.securegraph.EdgeBuilder;
import org.securegraph.Graph;
import org.securegraph.Vertex;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

/**
 * Base class for Columnar document mappings.
 */
public abstract class AbstractColumnDocumentMapping implements DocumentMapping {
    /**
     * The logger.
     */
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(AbstractColumnDocumentMapping.class);

    /**
     * The entity mappings for this CSV.
     */
    private final SortedSet<EntityMapping> entityMappings;

    /**
     * The relationship mappings for this CSV.
     */
    private final List<ColumnRelationshipMapping> relationshipMappings;

    /**
     * Create a new AbstractColumnDocumentMapping.
     *
     * @param entities      the entity mappings
     * @param relationships the relationship mappings
     */
    protected AbstractColumnDocumentMapping(final Map<String, ColumnEntityMapping> entities, final List<ColumnRelationshipMapping> relationships) {
        checkNotNull(entities, "At least one entity mapping must be provided.");
        checkArgument(!entities.isEmpty(), "At least one entity mapping must be provided.");
        SortedSet<EntityMapping> myEntities = new TreeSet<>();
        for (Map.Entry<String, ColumnEntityMapping> entry : entities.entrySet()) {
            myEntities.add(new EntityMapping(entry.getKey(), entry.getValue()));
        }
        this.entityMappings = Collections.unmodifiableSortedSet(myEntities);
        List<ColumnRelationshipMapping> myRels = new ArrayList<>();
        if (relationships != null) {
            myRels.addAll(relationships);
        }
        this.relationshipMappings = Collections.unmodifiableList(myRels);
    }

    @JsonProperty("entities")
    public final Map<String, ColumnEntityMapping> getEntities() {
        Map<String, ColumnEntityMapping> map = new HashMap<>();
        for (EntityMapping tm : entityMappings) {
            map.put(tm.getKey(), tm.getMapping());
        }
        return Collections.unmodifiableMap(map);
    }

    @JsonProperty("relationships")
    public final List<ColumnRelationshipMapping> getRelationships() {
        return relationshipMappings;
    }

    /**
     * Get an Iterable that returns the rows of the input document
     * that will be processed.
     *
     * @param input the input document
     * @return an Iterable over the rows of the input document
     * @throws IOException if an error occurs while retrieving the rows
     */
    protected abstract Iterable<Row> getRows(final InputStream input) throws IOException;

    @Override
    public void mapDocument(final InputStream inputDoc, final State state, final String vertexIdPrefix) throws IOException {
        Graph graph = state.getGraph();
        Visibility visibility = state.getData().getVisibility();
        Authorizations auths = state.getAuthorizations();

        for (Row row : getRows(inputDoc)) {
            Map<String, Vertex> entityMap;
            List<Vertex> newVertices;
            boolean skipLine;
            if (!row.isEmpty()) {
                entityMap = new HashMap<>();
                newVertices = new ArrayList<>();
                skipLine = false;
                for (EntityMapping entityMapping : entityMappings) {
                    String hash = entityMapping.getMapping().getVertexHash(row);
                    Vertex vertex = state.getVertex(hash);
                    if (vertex == null) {
                        try {
                            String vertexId = String.format("%s%s", vertexIdPrefix, hash);
                            VertexBuilder builder = graph.prepareVertex(vertexId, visibility);
                            entityMapping.getMapping().createVertex(row, builder, state);
                            vertex = builder.save(auths);
                            newVertices.add(vertex);
                            graph.flush();
                            state.createHasEntityEdge(vertex);
                            state.cacheVertex(hash, vertex);
                            state.addVertexToWorkspaceIfNeeded(vertex);
                        } catch (Exception e) {
                            LOGGER.debug("Error processing entity during mapping", e);
                            if (entityMapping.getMapping().isRequired()) {
                                LOGGER.info("Unable to map required entity, skipping row");
                                skipLine = true;
                                break;
                            }
                        }
                    }
                }
                // if all required entities were found, process relationships
                if (!skipLine) {
                    for (ColumnRelationshipMapping edgeMapping : relationshipMappings) {
                        RelationshipDef relDef = edgeMapping.defineRelationship(entityMap, row);
                        String edgeId = String.format("%s_%s_%s", relDef.getSource().getId(), relDef.getLabel(), relDef.getTarget().getId());
                        EdgeBuilder builder = graph.prepareEdge(edgeId, relDef.getSource(), relDef.getTarget(), relDef.getLabel(), visibility);
                        state.getData().setVisibilityJsonOnElement(builder);
                        builder.save(auths);
                    }
                } else {
                    // if line is skipped, remove any previously created vertices
                    for (Vertex v : newVertices) {
                        graph.removeVertex(v, auths);
                        state.removeVertex(v);
                    }
                }
                graph.flush();
            }
        }
    }

    private static class EntityMapping implements Comparable<EntityMapping> {
        private final String key;
        private final ColumnEntityMapping mapping;

        public EntityMapping(String key, ColumnEntityMapping mapping) {
            this.key = key;
            this.mapping = mapping;
        }

        public String getKey() {
            return key;
        }

        public ColumnEntityMapping getMapping() {
            return mapping;
        }

        @Override
        public int compareTo(final EntityMapping o) {
            return this.mapping.compareTo(o.mapping);
        }
    }

    public static interface Row {
        /**
         * Get the value of the requested column.
         * @param col the column index
         * @return the value of the column
         */
        String get(final int col);

        /**
         * @return the number of columns in this row
         */
        int getColumnCount();

        /**
         * @return true if this row is isEmpty
         */
        boolean isEmpty();

        /**
         * @return the row number of this row within the overall document
         */
        long getRowNumber();
    }

    protected static class RowIterable implements Iterable<Row> {
        private Iterator<Row> iterator;

        public RowIterable(final Iterator<Row> iter) {
            this.iterator = iter;
        }

        @Override
        public Iterator<Row> iterator() {
            return iterator;
        }
    }
}
