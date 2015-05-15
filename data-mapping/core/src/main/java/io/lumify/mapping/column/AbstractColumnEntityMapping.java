package io.lumify.mapping.column;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.properties.LumifyProperties;
import io.lumify.mapping.State;
import io.lumify.mapping.column.AbstractColumnDocumentMapping.Row;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.List;
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
     * The default value for the useExisting property.
     */
    public static final boolean DEFAULT_USE_EXISTING = false;

    /**
     * The UTF-8 charset used to generate hash values.
     */
    private static final Charset UTF8 = Charset.forName("UTF8");

    /**
     * The name of the property used as the label for this entity.
     */
    private final String labelPropertyName;

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
     * @param lblPropName the name of the property used as the label for this entity
     * @param rqd is this entity required? null for default
     */
    protected AbstractColumnEntityMapping(final Map<String, ColumnValue<?>> props, final String lblPropName, final Boolean rqd) {
        SortedMap<String, ColumnValue<?>> myProps = new TreeMap<>();
        if (props != null) {
            myProps.putAll(props);
        }
        this.properties = Collections.unmodifiableSortedMap(myProps);
        checkArgument(lblPropName != null && !lblPropName.trim().isEmpty(), "label property must be provided");
        this.labelPropertyName = lblPropName.trim();
        checkArgument(properties.containsKey(labelPropertyName), "label property must have a configured mapping in entity properties");
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

    public final String getLabelPropertyName() {
        return labelPropertyName;
    }

    @Override
    public final int getSortColumn() {
        return properties.get(labelPropertyName).getSortColumn();
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
            throw new LumifyException("Could not find MD5", ex);
        }
    }

    @Override
    public final void createVertex(final Row row, final VertexBuilder builder, final State state) {
        String conceptIRI = getConceptIRI(row);
        if (conceptIRI == null) {
            if (required) {
                throw new IllegalArgumentException(String.format("Sign and Concept URI for entity in column %d are required.",
                        getSortColumn()));
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
                    throw new LumifyException("", ex);
                }
            }
            if (!foundVisibilityJson) {
                LumifyProperties.VISIBILITY_JSON.setProperty(builder, state.getData().getVisibilityJson(), visibility);
            }
        }
    }

    @Override
    public int compareTo(final ColumnEntityMapping o) {
        return getSortColumn() - o.getSortColumn();
    }
}
