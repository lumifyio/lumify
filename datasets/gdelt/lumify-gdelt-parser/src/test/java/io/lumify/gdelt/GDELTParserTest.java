package io.lumify.gdelt;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GDELTParserTest {

    private GDELTParser parser;

    @Before
    public void setUp() {
        this.parser = new GDELTParser();
    }

    @Test
    public void testParseEventDataFields() throws Exception {
        GDELTEvent event = parser.parseLine("303291233\t20040703\t200407\t2004\t2004.5014\tEDU\tSTUDENT\tUSA\tALQ\tara\tALE\tHIN\tEDU\tRAD\tMOD\tBUS\tCOMPANY\tUSA\tALT\tart\tALA\tHIT\tBUS\tDAR\tDOM\t1\t040\t040\t04\t1\t1.0\t5\t1\t5\t2.24358974358974\t5\tStaffordshire, Staffordshire, United Kingdom\tUK\tUKM9\t52.8333\t-2\t-2608511\t5\tStaffordshire, Staffordshire, United Kingdom\tUK\tUKM9\t52.8333\t-2\t-2608511\t5\tStaffordshire, Staffordshire, United Kingdom\tUK\tUKM9\t52.8333\t-2\t-2608511\t20140701\thttp://www.thisismoney.co.uk/news/article-2675900/Student-loans-firm-shamed-axing-fake-legal-threats-Company-admits-sending-300-000-graduates-letters-past-decade.html?ITO=1490&ns_campaign=1490/RK=0");
        assertEquals("303291233", event.getGlobalEventId());
        assertEquals(new SimpleDateFormat("yyyyMMdd").parse("20040703"), event.getDateOfOccurrence());

        assertEquals("EDU", event.getActor1Code());
        assertEquals("STUDENT", event.getActor1Name());
        assertEquals("USA", event.getActor1CountryCode());
        assertEquals("ALQ", event.getActor1KnownGroupCode());
        assertEquals("ara", event.getActor1EthnicCode());
        assertEquals("ALE", event.getActor1Religion1Code());
        assertEquals("HIN", event.getActor1Religion2Code());
        assertEquals("EDU", event.getActor1Type1Code());
        assertEquals("RAD", event.getActor1Type2Code());
        assertEquals("MOD", event.getActor1Type3Code());

        assertEquals("BUS", event.getActor2Code());
        assertEquals("COMPANY", event.getActor2Name());
        assertEquals("USA", event.getActor2CountryCode());
        assertEquals("ALT", event.getActor2KnownGroupCode());
        assertEquals("art", event.getActor2EthnicCode());
        assertEquals("ALA", event.getActor2Religion1Code());
        assertEquals("HIT", event.getActor2Religion2Code());
        assertEquals("BUS", event.getActor2Type1Code());
        assertEquals("DAR", event.getActor2Type2Code());
        assertEquals("DOM", event.getActor2Type3Code());

        assertTrue(event.isRootEvent());
        assertEquals("040", event.getEventCode());
        assertEquals("040", event.getEventBaseCode());
        assertEquals("04", event.getEventRootCode());
        assertEquals(1, event.getQuadClass());
        assertEquals(1.0f, event.getGoldsteinScale(), 0.01);
        assertEquals(5, event.getNumMentions());
        assertEquals(1, event.getNumSources());
        assertEquals(5, event.getNumArticles());
        assertEquals(2.24358974358974, event.getAverageTone(), 0.01);

        assertEquals(5, event.getActor1GeoType());
        assertEquals("Staffordshire, Staffordshire, United Kingdom", event.getActor1GeoFullName());
        assertEquals("UK", event.getActor1GeoCountryCode());
        assertEquals("UKM9", event.getActor1GeoADM1Code());
        assertEquals(52.8333, event.getActor1GeoLatitude(), 0.01);
        assertEquals(-2, event.getActor1GeoLongitude(), 0.01);
        assertEquals(-2608511, event.getActor1GeoFeatureId());
    }
}
