package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkNotNull;

import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.mapping.LumifyDataMappingException;
import io.lumify.mapping.MappingState;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Hex;
import org.securegraph.Metadata;
import org.securegraph.VertexBuilder;
import org.securegraph.Visibility;

/**
 * Base class for ColumnEntityMappings.
 */
public abstract class AbstractColumnEntityMapping implements ColumnEntityMapping {
    /**
     * The default value for the required property.
     */
    public static boolean DEFAULT_REQUIRED = false;

    /**
     * The UTF-8 charset used to generate hash values.
     */
    private static final Charset UTF8 = Charset.forName("UTF8");

    /**
     * Is this entity required?
     */
    private final boolean required;

    /**
     * The properties of this entity.
     */
    private final SortedMap<String, ColumnValue<?>> properties;

    /**
     * Create a new ColumnEntityMapping.
     *
     * @param props the properties of this entity
     * @param rqd is this entity required? null for default
     */
    protected AbstractColumnEntityMapping(final Map<String, ColumnValue<?>> props, final Boolean rqd) {
        SortedMap<String, ColumnValue<?>> myProps = new TreeMap<>();
        if (props != null) {
            myProps.putAll(props);
        }
        this.properties = Collections.unmodifiableSortedMap(myProps);
        this.required = rqd != null ? rqd : DEFAULT_REQUIRED;
    }

    /**
     * Get the URI of the ontology concept for this entity.
     *
     * @param row the input row
     * @return the concept URI
     */
    protected abstract String getConceptIRI(final Row row);

    @Override
    public final boolean isRequired() {
        return required;
    }

    public final Map<String, ColumnValue<?>> getProperties() {
        return properties;
    }

    @Override
    public final String getVertexHash(final Row row) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            for (ColumnValue<?> val : properties.values()) {
                Object value = val.getValue(row);
                if (value != null) {
                    md5.update(value.toString().getBytes(UTF8));
                }
            }
            return Hex.encodeHexString(md5.digest());
        } catch (NoSuchAlgorithmException ex) {
            throw new LumifyDataMappingException("Could not find MD5", ex);
        }
    }

    @Override
    public final void createVertex(final Row row, final VertexBuilder builder, final MappingState state) {
        String conceptIRI = getConceptIRI(row);
        if (conceptIRI == null) {
            if (required) {
                throw new LumifyColumnMappingException(row, this, new IllegalArgumentException("Concept IRI is required"));
            }
        } else {
            Visibility visibility = state.getData().getVisibility();
            LumifyProperties.CONCEPT_TYPE.setProperty(builder, conceptIRI, visibility);
            boolean foundVisibilityJson = false;
            for (Map.Entry<String, ColumnValue<?>> prop : properties.entrySet()) {
                try {
                    String propertyKey = prop.getValue().getMultiKey();
                    if (propertyKey == null) {
                        propertyKey = state.getMultiKey();
                    }
                    String propertyName = prop.getKey();
                    checkNotNull(propertyName, "property 'name' is required");
                    if (LumifyProperties.VISIBILITY_JSON.getPropertyName().equals(propertyName)) {
                        foundVisibilityJson = true;
                    }

                    Metadata metadata = state.getData().createPropertyMetadata();
                    Object value = prop.getValue().getValue(row);
                    if (value != null) {
                        builder.addPropertyValue(propertyKey, propertyName, value, metadata, visibility);
                    }
                } catch (Exception ex) {
                    throw new LumifyColumnMappingException(row, this, prop.getValue(), ex);
                }
            }
            if (!foundVisibilityJson) {
                LumifyProperties.VISIBILITY_JSON.setProperty(builder, state.getData().getVisibilityJson(), visibility);
            }
        }
    }
}
