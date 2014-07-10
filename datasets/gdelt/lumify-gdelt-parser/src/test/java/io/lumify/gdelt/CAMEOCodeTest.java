package io.lumify.gdelt;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class CAMEOCodeTest {

    @Test
    public void testCountryLookup() {
        assertEquals("United States", CAMEOCode.getActorDescription("USA"));
    }

    @Test
    public void testEthnicLookup() {
        assertEquals("Afrikaners", CAMEOCode.getActorDescription("afr"));
    }

    @Test
    public void testEventCodeLookup() {
        assertEquals("Make empathetic comment", CAMEOCode.getEventDescription("018"));
    }

    @Test
    public void testGoldsteinScoreLookup() {
        assertEquals("3.4", CAMEOCode.getGoldsteinScore("0214"));
    }

    @Test
    public void testKnownGroupLookup() {
        assertEquals("European Union", CAMEOCode.getActorDescription("EEC"));
    }

    @Test
    public void testReligionLookup() {
        assertEquals("Christianity", CAMEOCode.getActorDescription("CHR"));
    }

    @Test
    public void testTypeLookup() {
        assertEquals("Human Rights", CAMEOCode.getActorDescription("HRI"));
    }

    @Test
    public void testWithLeadingOrTrailingSpaces() {
        assertEquals("Human Rights", CAMEOCode.getActorDescription(" HRI"));
        assertEquals("Human Rights", CAMEOCode.getActorDescription("HRI "));
        assertEquals("Human Rights", CAMEOCode.getActorDescription(" HRI  "));
    }
}
