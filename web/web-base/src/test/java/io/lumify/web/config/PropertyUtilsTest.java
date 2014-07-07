package io.lumify.web.config;

import io.lumify.core.config.PropertyUtils;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PropertyUtilsTest {

    private static final String PROPERTY_KEY = "foo";
    private static final String PROPERTY_VALUE = "bar";
    private Properties props;

    @Before
    public void setupTests() {
        props = new Properties();
    }

    @Test(expected = NullPointerException.class)
    public void testSetPropertyValueInvalidInstance() {
        PropertyUtils.setPropertyValue(null, PROPERTY_KEY, PROPERTY_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testSetPropertyValueInvalidProperty() {
        PropertyUtils.setPropertyValue(props, null, PROPERTY_VALUE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetPropertyValueEmptyProperty() {
        PropertyUtils.setPropertyValue(props, "", PROPERTY_VALUE);
    }

    @Test(expected = NullPointerException.class)
    public void testSetPropertyValueInvalidValue() {
        PropertyUtils.setPropertyValue(props, PROPERTY_KEY, null);
    }

    @Test
    public void testSetPropertyValue() {
        assertTrue(PropertyUtils.setPropertyValue(props, PROPERTY_KEY, PROPERTY_VALUE));
    }

    @Test
    public void testSetPropertyValueDuplicateEntry() {
        props.put(PROPERTY_KEY, PROPERTY_VALUE);
        assertFalse(PropertyUtils.setPropertyValue(props, PROPERTY_KEY, PROPERTY_VALUE));
    }
}
