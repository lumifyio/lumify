package com.altamiracorp.lumify.location;

import com.altamiracorp.bigtable.model.MockSession;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.geoNames.GeoName;
import com.altamiracorp.lumify.core.model.geoNames.GeoNameAdmin1CodeRepository;
import com.altamiracorp.lumify.core.model.geoNames.GeoNameCountryInfoRepository;
import com.altamiracorp.lumify.core.model.geoNames.GeoNameRepository;
import com.altamiracorp.lumify.core.model.termMention.TermMention;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.util.ModelUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(MockitoJUnitRunner.class)
public class SimpleTermLocationExtractorTest {
    private MockSession session;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private User user;

    @Mock
    private GeoNameRepository geoNameRepository;

    @Mock
    private GeoNameAdmin1CodeRepository geoNameAdmin1CodeRepository;

    @Mock
    private GeoNameCountryInfoRepository geoNameCountryInfoRepository;

    @Before
    public void before() {
        session = new MockSession();
        ModelUtil.initializeTables(session, user);
    }

    @Test
    public void testLookupReturnsHighestPopulation() throws Exception {
        SimpleTermLocationExtractor simpleTermLocationExtractor = new SimpleTermLocationExtractor(geoNameAdmin1CodeRepository, geoNameCountryInfoRepository);
        TermMention termIn = new TermMention();
        termIn.getMetadata().setSign("baltimore");

        GeoName geoName = new GeoName("baltimore", "1");
        geoName.getMetadata()
                .setLatitude(87.1)
                .setLongitude(-61.1)
                .setPopulation(200L);
        Mockito.when(geoNameRepository.findBestMatch("baltimore", user)).thenReturn(geoName);
        TermMention termOut = simpleTermLocationExtractor.getTermWithLocationLookup(geoNameRepository, termIn, user);
        assertNotNull(termOut);
        assertEquals("POINT(87.1,-61.1)", termOut.getMetadata().getGeoLocation());
    }
}
