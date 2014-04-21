package com.altamiracorp.lumify.core.version;

import org.junit.Test;
import static org.junit.Assert.*;

public class VersionServiceTest {
    private static final String EXPECTED_VERSION = "1.0-TEST";
    private static final String EXPECTED_BUILD_NUM = "12345";
    private static final Long EXPECTED_BUILD_TIME = 1387483761285L;
    
    @Test
    public void testPropertyLumifyVersionService() {
        VersionService service = new VersionService();
        assertEquals(EXPECTED_VERSION, service.getVersion());
        assertEquals(EXPECTED_BUILD_NUM, service.getScmBuildNumber());
        assertEquals(EXPECTED_BUILD_TIME, service.getUnixBuildTime());
    }
}
