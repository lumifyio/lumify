package io.lumify.mapping.xform;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractMappedValueTransformerTest<T> {
    private Map<String, T> testMap;
    private T nonNullDefaultValue;
    private String unmappedKey;

    @Before
    public final void setup() {
        testMap = getTestMap();
        nonNullDefaultValue = getNonNullDefault();
        if (nonNullDefaultValue == null) {
            throw new IllegalStateException("getNonNullDefault() must return a non-null value");
        }
        unmappedKey = getUnmappedKey();
        if (testMap.containsKey(unmappedKey)) {
            throw new IllegalStateException("getUnamppedKey() must return a key that does not exist in the test map");
        }
    }

    protected void doSetup() {
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testIllegalConstruction() {
        doTestConstructor("null value map, without default", null, NullPointerException.class);
        doTestConstructor("isEmpty value map, without default", Collections.EMPTY_MAP, IllegalArgumentException.class);
        doTestConstructor("null value map, with default", null, null, NullPointerException.class);
        doTestConstructor("isEmpty value map, with default", Collections.EMPTY_MAP, null, IllegalArgumentException.class);
    }

    @Test
    public void testLegalConstruction() {
        doTestConstructor("default not provided", testMap);
        doTestConstructor("null default provided", testMap, (T) null, (T) null);
        doTestConstructor("default provided", testMap, nonNullDefaultValue, nonNullDefaultValue);
    }

    @Test
    public void testTransform() {
        doTestTransform("no default provided", createXform(testMap), null);
        doTestTransform("null default provided", createXform(testMap, null), null);
        doTestTransform("non-null default provided", createXform(testMap, nonNullDefaultValue), nonNullDefaultValue);
    }

    private void doTestTransform(final String testPrefix, final AbstractMappedValueTransformer<T> xform, final T expDefault) {
        for (Map.Entry<String, T> entry : testMap.entrySet()) {
            doTestTransform(String.format("[%s] %s->%s", testPrefix, entry.getKey(), entry.getValue()), xform, entry.getKey(),
                    entry.getValue());
        }
        doTestTransform(String.format("[%s] [null]->DEFAULT[%s]", testPrefix, expDefault), xform, null, expDefault);
        doTestTransform(String.format("[%s] [unmapped]->DEFAULT[%s]", testPrefix, expDefault), xform, unmappedKey, expDefault);
    }

    /**
     * Get the test value map.
     * @return a test map
     */
    protected abstract Map<String, T> getTestMap();

    /**
     * Get a non-null default value.
     * @return a non-null default value
     */
    protected abstract T getNonNullDefault();

    /**
     * Get an unmapped key for the test map.
     * @return an unmapped test key
     */
    protected abstract String getUnmappedKey();

    /**
     * This method should use the single-argument constructor.
     * @param valueMap the map to configure
     * @return the constructed subclass
     */
    protected abstract AbstractMappedValueTransformer<T> createXform(final Map<String, T> valueMap);

    /**
     * This method should use the two-argument constructor.
     * @param valueMap the map to configure
     * @param defaultValue the default value
     * @return the constructed subclass
     */
    protected abstract AbstractMappedValueTransformer<T> createXform(final Map<String, T> valueMap, final T defaultValue);

    protected final void doTestConstructor(final String testName, final Map<String, T> valueMap, final Class<? extends Throwable> expError) {
        try {
            createXform(valueMap);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    protected final void doTestConstructor(final String testName, final Map<String, T> valueMap, final T defaultValue,
            final Class<? extends Throwable> expError) {
        try {
            createXform(valueMap, defaultValue);
            fail(String.format("[%s]: Expected %s", testName, expError.getName()));
        } catch (Exception e) {
            assertTrue(String.format("[%s]: Expected %s, got %s", testName, expError.getName(), e.getClass().getName()),
                    expError.isAssignableFrom(e.getClass()));
        }
    }

    protected final void doTestConstructor(final String testName, final Map<String, T> valueMap) {
        AbstractMappedValueTransformer<T> xform = createXform(valueMap);
        String msg = String.format("[%s]: ", testName);
        assertEquals(msg, valueMap, xform.getValueMap());
        assertNull(msg, xform.getDefaultValue());
    }

    protected final void doTestConstructor(final String testName, final Map<String, T> valueMap, final T defaultValue, final T expDefaultValue) {
        AbstractMappedValueTransformer<T> xform = createXform(valueMap, defaultValue);
        String msg = String.format("[%s]: ", testName);
        assertEquals(msg, valueMap, xform.getValueMap());
        assertEquals(msg, defaultValue, xform.getDefaultValue());
    }

    protected final void doTestTransform(final String testName, final AbstractMappedValueTransformer<T> xform, final String key,
            final T expected) {
        assertEquals(String.format("[%s]: ", testName), expected, xform.transform(key));
    }
}
