package io.lumify.core.model.properties.types;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import org.securegraph.*;
import org.securegraph.mutation.ElementMutation;
import org.securegraph.mutation.ExistingEdgeMutation;
import org.securegraph.mutation.ExistingElementMutation;

import java.util.Collections;
import java.util.Map;

/**
 * A LumifyProperty provides convenience methods for converting standard
 * property values to and from their raw types to the types required to
 * store them in a SecureGraph instance.
 *
 * @param <TRaw>   the raw value type for this property
 * @param <TGraph> the value type presented to SecureGraph for this property
 */
public abstract class LumifyProperty<TRaw, TGraph> {
    /**
     * The property propertyName.
     */
    private final String propertyName;

    /**
     * The raw conversion function.
     */
    private final Function<Object, TRaw> rawConverter;

    /**
     * Create a new LumifyProperty with the given propertyName.
     *
     * @param propertyName the property propertyName
     */
    protected LumifyProperty(final String propertyName) {
        this.propertyName = propertyName;
        this.rawConverter = new RawConverter();
    }

    /**
     * Convert the raw value to an appropriate value for storage
     * in SecureGraph.
     *
     * @param value the raw value
     * @return the SecureGraph value type representing the input value
     */
    public abstract TGraph wrap(final TRaw value);

    /**
     * Convert the SecureGraph value to its original raw type.
     *
     * @param value the SecureGraph value; may or may not be of type TGraph
     * @return the raw value represented by the input SecureGraph value
     * @throws ClassCastException if the provided value cannot be unwrapped
     */
    public abstract TRaw unwrap(final Object value);

    /**
     * Get the property propertyName for this property.
     *
     * @return the property propertyName
     */
    public final String getPropertyName() {
        return propertyName;
    }

    /**
     * Add a mutation to set this property to the provided value.
     *
     * @param mutation   the element mutation
     * @param value      the new property value
     * @param visibility the property visibility
     */
    public final void setProperty(final ElementMutation<?> mutation, final TRaw value, final Visibility visibility) {
        mutation.setProperty(propertyName, wrap(value), visibility);
    }

    /**
     * Add a mutation to set this property to the provided value.
     *
     * @param mutation   the element mutation
     * @param value      the new property value
     * @param metadata   the property metadata
     * @param visibility the property visibility
     */
    public final void setProperty(final ElementMutation<?> mutation, final TRaw value, final Metadata metadata, final Visibility visibility) {
        mutation.setProperty(propertyName, wrap(value), metadata, visibility);
    }

    /**
     * Set this property on the provided element.
     *
     * @param element    the element
     * @param value      the new property value
     * @param visibility the property visibility
     */
    public final void setProperty(final Element element, final TRaw value, final Visibility visibility, Authorizations authorizations) {
        element.setProperty(propertyName, wrap(value), visibility, authorizations);
    }

    /**
     * Set this property on the provided element.
     *
     * @param element    the element
     * @param value      the new property value
     * @param metadata   the property metadata
     * @param visibility the property visibility
     */
    public final void setProperty(final Element element, final TRaw value, final Metadata metadata, final Visibility visibility, Authorizations authorizations) {
        element.setProperty(propertyName, wrap(value), metadata, visibility, authorizations);
    }

    /**
     * Add a mutation to add a new value to this property.
     *
     * @param mutation   the element mutation
     * @param multiKey   the multi-valued property key
     * @param value      the new property value
     * @param visibility the property visibility
     */
    public final void addPropertyValue(final ElementMutation<?> mutation, final String multiKey, final TRaw value, final Visibility visibility) {
        mutation.addPropertyValue(multiKey, propertyName, wrap(value), visibility);
    }

    public final void addPropertyValue(final Element element, final String multiKey, final TRaw value, final Visibility visibility, Authorizations authorizations) {
        element.addPropertyValue(multiKey, propertyName, wrap(value), visibility, authorizations);
    }

    public final void addPropertyValue(final Element element, final String multiKey, final TRaw value, final Metadata metadata, final Visibility visibility, Authorizations authorizations) {
        element.addPropertyValue(multiKey, propertyName, wrap(value), metadata, visibility, authorizations);
    }

    /**
     * Add a mutation to add a new value to this property
     *
     * @param mutation   the element mutation
     * @param multiKey   the multi-valued property key
     * @param value      the new property value
     * @param metadata   the property metadata
     * @param visibility the property visibility
     */
    public final void addPropertyValue(final ElementMutation<?> mutation,
                                       final String multiKey,
                                       final TRaw value,
                                       final Metadata metadata,
                                       final Visibility visibility) {
        mutation.addPropertyValue(multiKey, propertyName, wrap(value), metadata, visibility);
    }

    /**
     * Get the value of this property from the provided Element.
     *
     * @param element the element
     * @return the value of this property on the given Element or null if it is not configured
     */
    public final TRaw getPropertyValue(final Element element) {
        Object value = element != null ? element.getPropertyValue(propertyName) : null;
        return value != null ? rawConverter.apply(value) : null;
    }

    public final TRaw getPropertyValue(final Element element, String propertyKey) {
        Object value = element != null ? element.getPropertyValue(propertyKey, propertyName) : null;
        return value != null ? rawConverter.apply(value) : null;
    }

    public final TRaw getPropertyValue(Property property) {
        Object value = property.getValue();
        return value != null ? rawConverter.apply(value) : null;
    }

    /**
     * Get all values of this property from the provided Element.
     *
     * @param element the element
     * @return an Iterable over the values of this property on the given Element
     */
    @SuppressWarnings("unchecked")
    public final Iterable<TRaw> getPropertyValues(final Element element) {
        Iterable<Object> values = element != null ? element.getPropertyValues(propertyName) : null;
        return values != null ? Iterables.transform(values, rawConverter) : Collections.EMPTY_LIST;
    }

    public boolean hasProperty(Element element, String propertyKey) {
        return element.getProperty(propertyKey, getPropertyName()) != null;
    }

    public TRaw getMetadataValue(Metadata metadata) {
        return unwrap(metadata.getValue(propertyName));
    }

    public TRaw getMetadataValue(Map<String, Object> metadata) {
        return unwrap(metadata.get(propertyName));
    }

    public TRaw getMetadataValue(Metadata metadata, TRaw defaultValue) {
        if (metadata.getEntry(propertyName) == null) {
            return defaultValue;
        }
        return unwrap(metadata.getValue(propertyName));
    }

    public void setMetadata(Metadata metadata, TRaw value, Visibility visibility) {
        metadata.add(propertyName, wrap(value), visibility);
    }

    public Property getProperty(Element element) {
        return element.getProperty(getPropertyName());
    }

    public Iterable<Property> getProperties(Element element) {
        return element.getProperties(getPropertyName());
    }

    public void removeProperty(Element element, String key, Authorizations authorizations) {
        element.removeProperty(key, getPropertyName(), authorizations);
    }

    public void removeProperty(Element element, Authorizations authorizations) {
        element.removeProperty(getPropertyName(), authorizations);
    }

    public void removeProperty(ExistingEdgeMutation m, final Visibility visibility) {
        m.removeProperty(getPropertyName(), visibility);
    }

    public void removeProperty(ExistingEdgeMutation m, String key, final Visibility visibility) {
        m.removeProperty(key, getPropertyName(), visibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(getPropertyName(), newVisibility);
    }

    public void alterVisibility(ExistingElementMutation<?> elementMutation, String propertyKey, Visibility newVisibility) {
        elementMutation.alterPropertyVisibility(propertyKey, getPropertyName(), newVisibility);
    }

    /**
     * Function that converts the values returned by the Vertex.getProperty()
     * methods to the configured TRaw type.
     */
    private class RawConverter implements Function<Object, TRaw> {
        @Override
        @SuppressWarnings("unchecked")
        public TRaw apply(final Object input) {
            return unwrap(input);
        }
    }
}
