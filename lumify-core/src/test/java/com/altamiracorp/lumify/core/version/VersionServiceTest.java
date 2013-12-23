/*
 * Copyright 2013 Altamira Corporation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
