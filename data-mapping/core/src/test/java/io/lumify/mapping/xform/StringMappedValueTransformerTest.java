package io.lumify.mapping.xform;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class StringMappedValueTransformerTest extends AbstractMappedValueTransformerTest<String> {
    private static final String UNMAPPED_KEY = "unmapped";
    private static final String DEFAULT_VALUE = "default";
    private static final Map<String, String> TEST_MAP;
    static {
        Map<String, String> map = new HashMap<String, String>();
        map.put("foo", "bar");
        map.put("fizz", "buzz");
        map.put("cheese", "burger");
        map.put("unknown", null);
        TEST_MAP = Collections.unmodifiableMap(map);
    }

    @Override
    protected Map<String, String> getTestMap() {
        return TEST_MAP;
    }

    @Override
    protected String getNonNullDefault() {
        return DEFAULT_VALUE;
    }

    @Override
    protected String getUnmappedKey() {
        return UNMAPPED_KEY;
    }

    @Override
    protected AbstractMappedValueTransformer<String> createXform(Map<String, String> valueMap) {
        return new StringMappedValueTransformer(valueMap);
    }

    @Override
    protected AbstractMappedValueTransformer<String> createXform(Map<String, String> valueMap, String defaultValue) {
        return new StringMappedValueTransformer(valueMap, defaultValue);
    }
}
