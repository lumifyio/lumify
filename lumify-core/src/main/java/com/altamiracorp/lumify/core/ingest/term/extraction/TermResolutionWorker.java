/*
 * Copyright 2014 Altamira Corporation.
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

package com.altamiracorp.lumify.core.ingest.term.extraction;

/**
 * TermResolutionWorkers should be executed after all TermExtractionWorkers have completed,
 * performing additional processing against all extracted Terms found in a particular Artifact.
 */
public interface TermResolutionWorker extends TermWorker {
    /**
     * Process the results of term extraction against a particular Artifact, adding
     * additional details or extracted terms as necessary.
     * @param results the extraction results
     * @return the updated results
     * @throws Exception if an error occurs during term resolution
     */
    TermExtractionResult resolveTerms(final TermExtractionResult results) throws Exception;
}
