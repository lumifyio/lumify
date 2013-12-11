package com.altamiracorp.lumify.core.model.geoNames;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.MockSession;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.RowKey;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.ModelUtil;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class GeoNameRepositoryTest {
    private MockSession session;
    private GeoNameRepository geoNameRepository;

    @Mock
    private User user;

    @Before
    public void before() {
        MockitoAnnotations.initMocks(this);

        session = new MockSession();
        ModelUtil.initializeTables(session, user);
        geoNameRepository = new GeoNameRepository(session);
    }

    @Test
    public void testFindByRowKey() {
        String rowKeyString = "boston:123";
        Row<RowKey> row = new Row<RowKey>(GeoName.TABLE_NAME, new RowKey(rowKeyString));

        ColumnFamily geoNameMetadataColumnFamily = new ColumnFamily(GeoNameMetadata.NAME);
        geoNameMetadataColumnFamily
                .set(GeoNameMetadata.NAME_COLUMN, "boston")
                .set(GeoNameMetadata.LATITUDE, 42.35)
                .set(GeoNameMetadata.LONGITUDE, -71.06)
                .set(GeoNameMetadata.POPULATION, 100L)
                .set(GeoNameMetadata.ADMIN_1_CODE, "testAdmin1Code")
                .set(GeoNameMetadata.ADMIN_2_CODE, "testAdmin2Code")
                .set(GeoNameMetadata.ADMIN_3_CODE, "testAdmin3Code")
                .set(GeoNameMetadata.ADMIN_4_CODE, "testAdmin4Code")
                .set(GeoNameMetadata.FEATURE_CLASS, "testFeatureClass")
                .set(GeoNameMetadata.FEATURE_CODE, "testFeatureCode")
                .set(GeoNameMetadata.COUNTRY_CODE, "testCountryCode")
                .set(GeoNameMetadata.ALTERNATE_COUNTRY_CODE, "testAlternateCountryCode")
                .set("extra", "textExtra");
        row.addColumnFamily(geoNameMetadataColumnFamily);

        ColumnFamily extraColumnFamily = new ColumnFamily("testExtraColumnFamily");
        extraColumnFamily
                .set("testExtraColumn", "testExtraValue");
        row.addColumnFamily(extraColumnFamily);

        session.tables.get(GeoName.TABLE_NAME).add(row);

        GeoName geoName = geoNameRepository.findByRowKey(rowKeyString, user.getModelUserContext());
        assertEquals(rowKeyString, geoName.getRowKey().toString());
        assertEquals(2, geoName.getColumnFamilies().size());

        GeoNameMetadata geoNameMetadata = geoName.getMetadata();
        assertEquals(GeoNameMetadata.NAME, geoNameMetadata.getColumnFamilyName());
        assertEquals("boston", geoNameMetadata.getName());
        assertEquals(42.35, geoNameMetadata.getLatitude().doubleValue(), 0.01);
        assertEquals(-71.06, geoNameMetadata.getLongitude().doubleValue(), 0.01);
        assertEquals(100L, geoNameMetadata.getPopulation().longValue());
        assertEquals("testAdmin1Code", geoNameMetadata.getAdmin1Code());
        assertEquals("testAdmin2Code", geoNameMetadata.getAdmin2Code());
        assertEquals("testAdmin3Code", geoNameMetadata.getAdmin3Code());
        assertEquals("testAdmin4Code", geoNameMetadata.getAdmin4Code());
        assertEquals("testFeatureClass", geoNameMetadata.getFeatureClass());
        assertEquals("testFeatureCode", geoNameMetadata.getFeatureCode());
        assertEquals("testCountryCode", geoNameMetadata.getCountryCode());
        assertEquals("testAlternateCountryCode", geoNameMetadata.getAlternateCountryCodes());
        assertEquals("textExtra", geoNameMetadata.get("extra").toString());

        ColumnFamily foundExtraColumnFamily = geoName.get("testExtraColumnFamily");
        assertNotNull("foundExtraColumnFamily", foundExtraColumnFamily);
        assertEquals("testExtraValue", foundExtraColumnFamily.get("testExtraColumn").toString());
    }

    @Test
    public void testSave() {
        GeoName geoName = new GeoName("Boston", "123");

        geoName.getMetadata()
                .setName("Boston")
                .setLatitude(42.35)
                .setLongitude(-71.06)
                .setCountryCode("testCountryCode")
                .setAlternateCountryCodes("testAlternateCountryCodes")
                .setAdmin1Code("testAdmin1Code")
                .setAdmin2Code("testAdmin2Code")
                .setAdmin3Code("testAdmin3Code")
                .setAdmin4Code("testAdmin4Code")
                .setPopulation(100L)
                .setFeatureClass("testFeatureClass")
                .setFeatureCode("testFeatureCode")
                .set("testExtra", "testExtraValue");

        geoName.addColumnFamily(
                new ColumnFamily("testExtraColumnFamily")
                        .set("testExtraColumn", "testExtraValue"));

        geoNameRepository.save(geoName, user.getModelUserContext());

        assertEquals(1, session.tables.get(GeoName.TABLE_NAME).size());
        Row row = session.tables.get(GeoName.TABLE_NAME).get(0);
        assertEquals(RowKeyHelper.buildMinor("boston", "123"), row.getRowKey().toString());

        assertEquals(2, row.getColumnFamilies().size());

        ColumnFamily geoNameMetadataColumnFamily = row.get(GeoNameMetadata.NAME);
        assertEquals(GeoNameMetadata.NAME, geoNameMetadataColumnFamily.getColumnFamilyName());
        assertEquals("Boston", geoNameMetadataColumnFamily.get(GeoNameMetadata.NAME_COLUMN).toString());
        assertEquals(42.35, geoNameMetadataColumnFamily.get(GeoNameMetadata.LATITUDE).toDouble().doubleValue(), 0.01);
        assertEquals(-71.06, geoNameMetadataColumnFamily.get(GeoNameMetadata.LONGITUDE).toDouble().doubleValue(), 0.01);
        assertEquals("testCountryCode", geoNameMetadataColumnFamily.get(GeoNameMetadata.COUNTRY_CODE).toString());
        assertEquals("testAlternateCountryCodes", geoNameMetadataColumnFamily.get(GeoNameMetadata.ALTERNATE_COUNTRY_CODE).toString());
        assertEquals("testAdmin1Code", geoNameMetadataColumnFamily.get(GeoNameMetadata.ADMIN_1_CODE).toString());
        assertEquals("testAdmin2Code", geoNameMetadataColumnFamily.get(GeoNameMetadata.ADMIN_2_CODE).toString());
        assertEquals("testAdmin3Code", geoNameMetadataColumnFamily.get(GeoNameMetadata.ADMIN_3_CODE).toString());
        assertEquals("testAdmin4Code", geoNameMetadataColumnFamily.get(GeoNameMetadata.ADMIN_4_CODE).toString());
        assertEquals(100L, geoNameMetadataColumnFamily.get(GeoNameMetadata.POPULATION).toLong().longValue());
        assertEquals("testFeatureClass", geoNameMetadataColumnFamily.get(GeoNameMetadata.FEATURE_CLASS).toString());
        assertEquals("testFeatureCode", geoNameMetadataColumnFamily.get(GeoNameMetadata.FEATURE_CODE).toString());
        assertEquals("testExtraValue", geoNameMetadataColumnFamily.get("testExtra").toString());

        ColumnFamily extraColumnFamily = row.get("testExtraColumnFamily");
        assertNotNull("extraColumnFamily", extraColumnFamily);
        assertEquals(1, extraColumnFamily.getColumns().size());
        assertEquals("testExtraValue", extraColumnFamily.get("testExtraColumn").toString());
    }

    @Test
    public void testFindBestMatch() {
        GeoName geoName1 = new GeoName("Boston", "123");
        geoName1.getMetadata()
                .setName("Boston1")
                .setPopulation(100L);
        geoNameRepository.save(geoName1, user.getModelUserContext());

        GeoName geoName2 = new GeoName("Boston", "234");
        geoName2.getMetadata()
                .setName("Boston2")
                .setPopulation(300L);
        geoNameRepository.save(geoName2, user.getModelUserContext());

        GeoName match = geoNameRepository.findBestMatch("boston", user);
        assertEquals("Boston2", match.getMetadata().getName());
    }

    @Test
    public void testFindBestMatchNoMatches() {
        GeoName match = geoNameRepository.findBestMatch("boston", user);
        assertNull("Found a match but shouldn't", match);
    }

    @Test
    public void testFindBestMatchWithNulls() {
        GeoName geoName1 = new GeoName("Boston", "123");
        geoName1.getMetadata()
                .setName("Boston1")
                .setPopulation(100L);
        geoNameRepository.save(geoName1, user.getModelUserContext());

        GeoName geoName2 = new GeoName("Boston", "234");
        geoName2.getMetadata()
                .setName("Boston2");
        geoNameRepository.save(geoName2, user.getModelUserContext());

        GeoName match = geoNameRepository.findBestMatch("boston", user);
        assertEquals("Boston1", match.getMetadata().getName());
    }
}