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

package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.lumify.core.model.audit.AuditRepository;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.termMention.TermMentionRepository;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.user.UserProvider;
import com.altamiracorp.securegraph.ElementMutation;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;

import java.util.Date;
import java.util.Iterator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Base class for processes that identify and create Artifacts,
 * Entities and Relationships from source documents.  This class
 * provides access to the services required for CRUD and audit
 * operations in the Lumify system.
 */
public abstract class BaseArtifactProcessor {
    /**
     * The User this processor is executing as.
     */
    private User user;

    /**
     * The Ontology Repository.
     */
    private OntologyRepository ontologyRepository;

    /**
     * The Graph Repository.
     */
    private Graph graph;

    /**
     * The Audit Repository.
     */
    private AuditRepository auditRepository;

    /**
     * The Term Mention Repository.
     */
    private TermMentionRepository termMentionRepository;

    /**
     * The Work Queue Repository.
     */
    private WorkQueueRepository workQueueRepository;

    protected final OntologyRepository getOntologyRepository() {
        return ontologyRepository;
    }

    @Inject
    public final void setOntologyRepository(final OntologyRepository ontologyRepository) {
        this.ontologyRepository = ontologyRepository;
    }

    protected final Graph getGraph() {
        return graph;
    }

    @Inject
    public final void setGraph(final Graph graph) {
        this.graph = graph;
    }

    protected final AuditRepository getAuditRepository() {
        return auditRepository;
    }

    @Inject
    public final void setAuditRepository(final AuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    protected final TermMentionRepository getTermMentionRepository() {
        return termMentionRepository;
    }

    @Inject
    public final void setTermMentionRepository(final TermMentionRepository termMentionRepository) {
        this.termMentionRepository = termMentionRepository;
    }

    protected final WorkQueueRepository getWorkQueueRepository() {
        return workQueueRepository;
    }

    @Inject
    public final void setWorkQueueRepository(final WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }

    protected final User getUser() {
        return user;
    }

    @Inject
    public final void setUser(final UserProvider user) { this.user = user.getSystemUser(); }

    public ElementMutation<Vertex> findOrPrepareArtifactVertex(String rowKey) {
        return findOrPrepareArtifactVertex(getGraph(), getUser(), rowKey);
    }

    public static ElementMutation<Vertex> findOrPrepareArtifactVertex(Graph graph, User user, String rowKey) {
        ElementMutation<Vertex> vertex;
        checkNotNull(rowKey, "rowKey is required to save artifact");
        Iterator<Vertex> existingVertices = graph.query(user.getAuthorizations())
                .has(PropertyName.ROW_KEY.toString(), rowKey)
                .vertices()
                .iterator();
        if (existingVertices.hasNext()) {
            vertex = existingVertices.next().prepareMutation();
            if (existingVertices.hasNext()) {
                throw new RuntimeException("Found multiple vertex matches for " + rowKey);
            }
        } else {
            Visibility visibility = new Visibility("");
            vertex = graph.prepareVertex(visibility, user.getAuthorizations())
                    .setProperty(PropertyName.CREATE_DATE.toString(), new Date(), visibility)
                    .setProperty(PropertyName.ROW_KEY.toString(), rowKey, visibility);
        }
        return vertex;
    }
}
