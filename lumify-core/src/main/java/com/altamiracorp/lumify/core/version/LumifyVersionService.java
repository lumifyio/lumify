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

/**
 * This service provides methods for retrieving information about
 * the current Lumify version and build metadata.
 */
public interface LumifyVersionService {
    /**
     * Get the timestamp when this component was built.
     * @return the Unix-time at which this build occurred or <code>null</code> if unknown
     */
    Long getUnixBuildTime();
    
    /**
     * Get the Lumify version.
     * @return the current Lumify version or <code>null</code> if unknown
     */
    String getVersion();
    
    /**
     * Get the unique identifier of this version of the
     * code in the SCM repository.  This may be the git SHA-1 hash,
     * Subversion commit number or equivalent for other SCM systems.
     * @return the SCM build number for the state of the code when this module was built or <code>null</code> if unknown
     */
    String getScmBuildNumber();
}
