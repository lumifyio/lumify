package com.altamiracorp.lumify.core.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class GeoLocationTest {
    @Test
    public void testGetLatitude() {
        Double actual = GeoLocation.getLatitude("POINT(123.4,456.6)");
        assertEquals((Double)123.4, actual);
    }

    @Test
    public void testGetLongitude() {
        Double actual = GeoLocation.getLongitude("POINT(123.4,-456.6)");
        assertEquals((Double)(-456.6), actual);
    }

    @Test
    public void testSetLatitude() {
        String actual = GeoLocation.getGeoLocation(123.4,-456.6);
        assertEquals("POINT(123.4,-456.6)", actual);
    }
}
