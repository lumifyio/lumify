package com.altamiracorp.lumify.core.model.ontology;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.TimeZone;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class PropertyTest {
    @Test
    public void testDateFormatTimeZoneInUTC() {
        assertEquals(TimeZone.getTimeZone("UTC"), OntologyProperty.DATE_FORMAT.getTimeZone());
    }
}
