package io.lumify.mapping.column;

import io.lumify.core.ingest.term.extraction.TermMention;
import io.lumify.mapping.DocumentMapping;
import org.securegraph.Visibility;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for ColumnEntityMappings.
 */
public abstract class AbstractColumnEntityMapping implements ColumnEntityMapping {
    /**
     * The default value for the required property.
     */
    public static boolean DEFAULT_REQUIRED = false;

    /**
     * The default value for the useExisting property.
     */
    public static final boolean DEFAULT_USE_EXISTING = false;

    /**
     * The ColumnValue used as the ID of this entity.
     */
    private final ColumnValue<String> idColumn;

    /**
     * The ColumnValue used as the sign of this entity.
     */
    private final ColumnValue<String> signColumn;

    /**
     * Is this entity required?
     */
    private final boolean required;

    /**
     * Should existing entities be used if found or should a
     * new entity be created for each mapped column?
     */
    private final boolean useExisting;

    /**
     * The properties of this entity.
     */
    private final Map<String, ColumnValue<?>> properties;

    /**
     * Create a new ColumnEntityMapping.
     *
     * @param idCol       the ColumnValue providing the ID of this entity; if null, the system will use an auto-generated ID
     * @param signCol     the ColumnValue providing the sign of this entity
     * @param props       the properties of this entity
     * @param useExisting should existing entities be reused? null for default
     * @param required    is this entity required? null for default
     */
    public AbstractColumnEntityMapping(final ColumnValue<String> idCol, final ColumnValue<String> signCol, final Map<String, ColumnValue<?>> props,
                                       final Boolean useExisting, final Boolean required) {
        checkNotNull(signCol, "sign column must be provided");
        this.idColumn = idCol;
        this.signColumn = signCol;
        Map<String, ColumnValue<?>> myProps = new HashMap<String, ColumnValue<?>>();
        if (props != null) {
            myProps.putAll(props);
        }
        this.properties = Collections.unmodifiableMap(myProps);
        this.useExisting = useExisting != null ? useExisting : DEFAULT_USE_EXISTING;
        this.required = required != null ? required : DEFAULT_REQUIRED;
    }

    /**
     * Get the URI of the ontology concept for this entity.
     *
     * @param row the input row
     * @return the concept URI
     */
    protected abstract String getConceptURI(final List<String> row);

    public final ColumnValue<?> getIdColumn() {
        return idColumn;
    }

    public final ColumnValue<String> getSignColumn() {
        return signColumn;
    }

    @Override
    public final boolean isRequired() {
        return required;
    }

    public final boolean isUseExisting() {
        return useExisting;
    }

    public final Map<String, ColumnValue<?>> getProperties() {
        return properties;
    }

    @Override
    public final int getSortColumn() {
        return signColumn.getSortColumn();
    }

    /**
     * Generate a TermMention, with all associated properties, from the columns
     * of a row in a columnar document.
     *
     * @param row        the columns of the input row
     * @param offset     the current document offset
     * @param processId  the ID of the process reading this document
     * @param visibility
     * @return the generated TermMention
     */
    @Override
    public final TermMention mapTerm(final List<String> row, final int offset, final String processId, String propertyKey, Visibility visibility) {
        String id = idColumn != null ? idColumn.getValue(row) : null;
        String sign = signColumn.getValue(row);
        checkNotNull(sign, "sign cannot be null (offset: " + offset + ")");
        String conceptURI = getConceptURI(row);
        TermMention mention;
        if (sign == null || sign.trim().isEmpty() || conceptURI == null) {
            if (required) {
                throw new IllegalArgumentException(String.format("Sign and Concept URI for entity in column %d are required.",
                        getSortColumn()));
            } else {
                mention = null;
            }
        } else {
            int start = offset;
            int end = offset + sign.length();
            TermMention.Builder builder = new TermMention.Builder(start, end, sign, conceptURI, propertyKey, visibility)
                    .id(id)
                    .useExisting(useExisting)
                    .resolved(true)
                    .process(processId);
            for (Map.Entry<String, ColumnValue<?>> prop : properties.entrySet()) {
                Object value = prop.getValue().getValue(row);
                if (value != null) {
                    builder.addProperty(DocumentMapping.class.getName(), prop.getKey(), value);
                }
            }
            mention = builder.build();
        }
        return mention;
    }

    @Override
    public int compareTo(final ColumnEntityMapping o) {
        return getSortColumn() - o.getSortColumn();
    }
}
