package io.lumify.gdelt;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class GDELTParserTest {

    private GDELTParser parser;
    private GDELTEvent event;

    @Before
    public void setUp() throws ParseException {
        this.parser = new GDELTParser();
        this.event = this.parser.parseLine("303291233\t20040703\t200407\t2004\t2004.5014\tEDU\tSTUDENT\tUSA\tALQ\tara\tALE\tHIN\tEDU\tRAD\tMOD\tBUS\tCOMPANY\tUSA\tALT\tart\tALA\tHIT\tBUS\tDAR\tDOM\t1\t040\t040\t04\t1\t1.0\t5\t1\t5\t2.24358974358974\t5\tStaffordshire, Staffordshire, United Kingdom\tUK\tUKM9\t52.8333\t-2\t-2608511\t6\tLondon, United Kingdom\tUK2\tUKM92\t62.8333\t-3\t-3608511\t7\tChelsea, United Kingdom\tUK3\tUKM93\t72.8333\t-4\t-4608511\t20140701\thttp://www.thisismoney.co.uk/news/article-2675900/Student-loans-firm-shamed-axing-fake-legal-threats-Company-admits-sending-300-000-graduates-letters-past-decade.html?ITO=1490&ns_campaign=1490/RK=0");
    }

    @Test
    public void testParseEventDataFields() throws Exception {
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
        assertEquals("-2608511", event.getActor1GeoFeatureId());

        assertEquals(6, event.getActor2GeoType());
        assertEquals("London, United Kingdom", event.getActor2GeoFullName());
        assertEquals("UK2", event.getActor2GeoCountryCode());
        assertEquals("UKM92", event.getActor2GeoADM1Code());
        assertEquals(62.8333, event.getActor2GeoLatitude(), 0.01);
        assertEquals(-3, event.getActor2GeoLongitude(), 0.01);
        assertEquals("-3608511", event.getActor2GeoFeatureId());

        assertEquals(7, event.getActionGeoType());
        assertEquals("Chelsea, United Kingdom", event.getActionGeoFullName());
        assertEquals("UK3", event.getActionGeoCountryCode());
        assertEquals("UKM93", event.getActionGeoADM1Code());
        assertEquals(72.8333, event.getActionGeoLatitude(), 0.01);
        assertEquals(-4, event.getActionGeoLongitude(), 0.01);
        assertEquals("-4608511", event.getActionGeoFeatureId());

        assertEquals(new SimpleDateFormat("yyyyMMdd").parse("20140701"), event.getDateAdded());
        assertEquals("http://www.thisismoney.co.uk/news/article-2675900/Student-loans-firm-shamed-axing-fake-legal-threats-Company-admits-sending-300-000-graduates-letters-past-decade.html?ITO=1490&ns_campaign=1490/RK=0", event.getSourceUrl());
    }

    @Test
    public void testActor1() {
        GDELTActor actor = event.getActor1();
        assertEquals("EDU", actor.getCode());
        assertEquals("STUDENT", actor.getName());
        assertEquals("USA", actor.getCountryCode());
        assertEquals("ALQ", actor.getKnownGroupCode());
        assertEquals("ara", actor.getEthnicCode());
        assertEquals("ALE", actor.getReligion1Code());
        assertEquals("HIN", actor.getReligion2Code());
        assertEquals("EDU", actor.getType1Code());
        assertEquals("RAD", actor.getType2Code());
        assertEquals("MOD", actor.getType3Code());
    }

    @Test
    public void testActor2() {
        GDELTActor actor = event.getActor2();
        assertEquals("BUS", actor.getCode());
        assertEquals("COMPANY", actor.getName());
        assertEquals("USA", actor.getCountryCode());
        assertEquals("ALT", actor.getKnownGroupCode());
        assertEquals("art", actor.getEthnicCode());
        assertEquals("ALA", actor.getReligion1Code());
        assertEquals("HIT", actor.getReligion2Code());
        assertEquals("BUS", actor.getType1Code());
        assertEquals("DAR", actor.getType2Code());
        assertEquals("DOM", actor.getType3Code());
    }

    @Ignore // Remove the annotation when making changes to parsing code. The test takes a long time to run.
    @Test
    public void testParsingLargeFile() throws ParseException, IOException {
        InputStream is = GDELTParserTest.class.getResourceAsStream("20140701.export.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                parser.parseLine(line);
            }
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                System.err.println("Failed to close reader: " + e.toString());
            }
        }
    }
}
