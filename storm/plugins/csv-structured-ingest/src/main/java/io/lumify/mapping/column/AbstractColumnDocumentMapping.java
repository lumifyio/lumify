package io.lumify.mapping.column;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.lumify.core.ingest.term.extraction.TermExtractionResult;
import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.core.ingest.term.extraction.TermRelationship;
import io.lumify.core.ingest.term.extraction.VertexRelationship;
import io.lumify.mapping.DocumentMapping;
import org.securegraph.Visibility;

import java.io.IOException;
import java.io.Reader;
import java.util.*;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for Columnar document mappings.
 */
public abstract class AbstractColumnDocumentMapping implements DocumentMapping {
    /**
     * The subject for this mapping.
     */
    private final String subject;

    /**
     * The entity mappings for this CSV.
     */
    private final SortedSet<EntityMapping> entityMappings;

    /**
     * The relationship mappings for this CSV.
     */
    private final List<ColumnRelationshipMapping> relationshipMappings;

    private final List<ColumnVertexRelationshipMapping> vertexRelationshipMappings;

    /**
     * Create a new AbstractColumnDocumentMapping.
     *
     * @param subject       the subject for the ingested Document
     * @param entities      the entity mappings
     * @param relationships the relationship mappings
     */
    protected AbstractColumnDocumentMapping(
            final String subject,
            final Map<String, ColumnEntityMapping> entities,
            final List<ColumnRelationshipMapping> relationships,
            final List<ColumnVertexRelationshipMapping> vertexRelationships) {
        checkArgument(subject != null && !subject.trim().isEmpty(), "Subject must be provided.");
        checkNotNull(entities, "At least one entity mapping must be provided.");
        checkArgument(!entities.isEmpty(), "At least one entity mapping must be provided.");
        this.subject = subject.trim();
        SortedSet<EntityMapping> myEntities = new TreeSet<EntityMapping>();
        for (Map.Entry<String, ColumnEntityMapping> entry : entities.entrySet()) {
            myEntities.add(new EntityMapping(entry.getKey(), entry.getValue()));
        }
        this.entityMappings = Collections.unmodifiableSortedSet(myEntities);

        List<ColumnRelationshipMapping> myRels = new ArrayList<ColumnRelationshipMapping>();
        if (relationships != null) {
            myRels.addAll(relationships);
        }
        this.relationshipMappings = Collections.unmodifiableList(myRels);

        List<ColumnVertexRelationshipMapping> myVertexRels = new ArrayList<ColumnVertexRelationshipMapping>();
        if (vertexRelationships != null) {
            myVertexRels.addAll(vertexRelationships);
        }
        this.vertexRelationshipMappings = Collections.unmodifiableList(myVertexRels);
    }

    @JsonProperty("subject")
    @Override
    public final String getSubject() {
        return subject;
    }

    @JsonProperty("entities")
    public final Map<String, ColumnEntityMapping> getEntities() {
        Map<String, ColumnEntityMapping> map = new HashMap<String, ColumnEntityMapping>();
        for (EntityMapping tm : entityMappings) {
            map.put(tm.getKey(), tm.getMapping());
        }
        return Collections.unmodifiableMap(map);
    }

    @JsonProperty("relationships")
    public final List<ColumnRelationshipMapping> getRelationships() {
        return relationshipMappings;
    }

    @JsonProperty("vertexRelationships")
    public final List<ColumnVertexRelationshipMapping> getVertexRelationships() {
        return vertexRelationshipMappings;
    }

    /**
     * Get an Iterable that returns the rows of the input document
     * that will be processed.
     *
     * @param reader a Reader over the input document
     * @return an Iterable over the rows of the input document
     * @throws IOException if an error occurs while retrieving the rows
     */
    protected abstract Iterable<Row> getRows(final Reader reader) throws IOException;

    @Override
    public final TermExtractionResult mapDocument(final Reader inputDoc, final String processId, String propertyKey, Visibility visibility) throws IOException {
        TermExtractionResult result = new TermExtractionResult();
        Iterator<TermExtractionResult> rowResults = mapDocumentElements(inputDoc, processId, propertyKey, visibility);
        while (rowResults.hasNext()) {
            TermExtractionResult rowResult = rowResults.next();
            result.addAllTermMentions(rowResult.getTermMentions());
            result.addAllRelationships(rowResult.getRelationships());
            result.addAllVertexRelationships(rowResult.getVertexRelationships());
        }
        return result;
    }

    @Override
    public final Iterator<TermExtractionResult> mapDocumentElements(final Reader inputDoc, final String processId, final String propertyKey,
                                                                    final Visibility visibility) throws IOException {
        Iterable<Row> rows = getRows(inputDoc);
        return new RowResultIterator(rows.iterator(), processId, propertyKey, visibility);
    }

    private class RowResultIterator implements Iterator<TermExtractionResult> {
        private final Iterator<Row> rows;
        private final String processId;
        private final String propertyKey;
        private final Visibility visibility;

        public RowResultIterator(final Iterator<Row> rows, final String processId, final String propertyKey, final Visibility visibility) {
            this.rows = rows;
            this.processId = processId;
            this.propertyKey = propertyKey;
            this.visibility = visibility;
        }

        @Override
        public boolean hasNext() {
            return rows.hasNext();
        }

        @Override
        public TermExtractionResult next() {
            Row row = rows.next();
            TermExtractionResult results = new TermExtractionResult();
            int offset = row.getOffset();
            List<String> columns = row.getColumns();
            Map<String, TermMention> termMap;
            TermMention mention;
            TermMention tgtMention;
            int lastCol;
            int currentCol;
            boolean skipLine;
            // if columns are null, stop processing
            if (columns != null) {
                // extract all identified Terms, adding them to the results and
                // mapping them by the configured map ID for relationship discovery
                List<TermMention> mentions = new ArrayList<TermMention>();
                termMap = new HashMap<String, TermMention>();
                lastCol = 0;
                skipLine = false;
                for (EntityMapping termMapping : entityMappings) {
                    ColumnEntityMapping colMapping = termMapping.getMapping();
                    // term mappings are ordered by column number; update offset
                    // so it is set to the start of the column for the current term
                    currentCol = colMapping.getSortColumn();
                    for (/* no precondition */; lastCol < currentCol; lastCol++) {
                        offset += (columns.get(lastCol) != null ? columns.get(lastCol).length() : 0) + 1;
                    }
                    try {
                        mention = colMapping.mapTerm(columns, offset, processId, propertyKey, visibility);
                        if (mention != null) {
                            // no need to update offset here, it will get updated by the block
                            // above when the next term is processed or, if this is the last term,
                            // it will be set to the proper offset for the next line
                            termMap.put(termMapping.getKey(), mention);
                            mentions.add(mention);
                        }
                    } catch (Exception e) {
                        if (colMapping.isRequired()) {
                            // skip line
                            skipLine = true;
                            break;
                        }
                    }
                }
                if (!skipLine) {
                    // parse all configured relationships, generating the relationship only
                    // if both Terms were successfully extracted
                    List<TermRelationship> relationships = new ArrayList<TermRelationship>();
                    for (ColumnRelationshipMapping relMapping : relationshipMappings) {
                        TermRelationship rel = relMapping.createRelationship(termMap, columns, visibility);
                        if (rel != null) {
                            relationships.add(rel);
                        }
                    }

                    List<VertexRelationship> vertexRelationships = new ArrayList<VertexRelationship>();
                    for (ColumnVertexRelationshipMapping relMapping : vertexRelationshipMappings) {
                        VertexRelationship rel = relMapping.createVertexRelationship(termMap, columns, visibility);
                        if (rel != null) {
                            vertexRelationships.add(rel);
                        }
                    }

                    results.addAllTermMentions(mentions);
                    results.addAllRelationships(relationships);
                    results.addAllVertexRelationships(vertexRelationships);
                }
            }
            return results;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Remove is not supported.");
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

    public static final class Row {
        private final int offset;
        private final List<String> columns;

        public Row(final int offset, final List<String> columns) {
            this.offset = offset;
            this.columns = Collections.unmodifiableList(new ArrayList<String>(columns));
        }

        public int getOffset() {
            return offset;
        }

        public List<String> getColumns() {
            return columns;
        }

        @Override
        public String toString() {
            return String.format("[%d]: %s", offset, columns);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 13 * hash + this.offset;
            hash = 13 * hash + (this.columns != null ? this.columns.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Row other = (Row) obj;
            if (this.offset != other.offset) {
                return false;
            }
            if (this.columns != other.columns && (this.columns == null || !this.columns.equals(other.columns))) {
                return false;
            }
            return true;
        }
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
