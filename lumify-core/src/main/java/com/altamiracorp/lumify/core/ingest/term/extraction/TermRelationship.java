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
 * A relationship between two mentioned terms.
 */
public class TermRelationship {
    private final TermMention sourceTermMention;
    private final TermMention destTermMention;
    private final String label;

    public TermRelationship(TermMention sourceTermMention, TermMention destTermMention, String label) {
        this.sourceTermMention = sourceTermMention;
        this.destTermMention = destTermMention;
        this.label = label;
    }

    public TermMention getSourceTermMention() {
        return sourceTermMention;
    }

    public TermMention getDestTermMention() {
        return destTermMention;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 13 * hash + (this.sourceTermMention != null ? this.sourceTermMention.hashCode() : 0);
        hash = 13 * hash + (this.destTermMention != null ? this.destTermMention.hashCode() : 0);
        hash = 13 * hash + (this.label != null ? this.label.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TermRelationship other = (TermRelationship) obj;
        if (this.sourceTermMention != other.sourceTermMention && (this.sourceTermMention == null || !this.sourceTermMention.equals(other.sourceTermMention))) {
            return false;
        }
        if (this.destTermMention != other.destTermMention && (this.destTermMention == null || !this.destTermMention.equals(other.destTermMention))) {
            return false;
        }
        if ((this.label == null) ? (other.label != null) : !this.label.equals(other.label)) {
            return false;
        }
        return true;
    }
}
