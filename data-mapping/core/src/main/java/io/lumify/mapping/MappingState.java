package io.lumify.mapping;

import io.lumify.core.ingest.graphProperty.GraphPropertyWorkData;
import org.securegraph.Authorizations;
import org.securegraph.Graph;
import org.securegraph.Vertex;

/**
 * Maintains State information during a document mapping. This class
 * encapsulates the GraphPropertyWorkData of the input job and provides
 * a Vertex cache that can be used by DocumentMapping implementations to
 * avoid creating duplicate Vertices.
 */
public interface MappingState {
    /**
     * @return the work data for the current mapping process
     */
    GraphPropertyWorkData getData();

    /**
     * @return the Graph
     */
    Graph getGraph();

    /**
     * @return the ingest authorization tokens
     */
    Authorizations getAuthorizations();

    /**
     * @return the default key used for multi-valued properties
     */
    String getMultiKey();

    /**
     * Get the cached Vertex ID for the given hash.
     * @param hash the Vertex hash
     * @return the cached Vertex ID or <code>null</code> if unknown
     */
    String getCachedVertexId(final String hash);

    /**
     * Cache the Vertex by its hash value.
     * @param hash the Vertex hash
     * @param vertex the Vertex to cache
     */
    void cacheVertex(final String hash, final Vertex vertex);

    /**
     * Remove the Vertex from the cache if it is found.
     * @param vertex the Vertex to remove
     */
    void removeVertex(final Vertex vertex);

    /**
     * Get the cached Vertex for the given hash.
     * @param hash the Vertex hash
     * @return the cached Vertex or <code>null</code> if unknown
     */
    Vertex getVertex(final String hash);

    /**
     * Creates a "has entity" link from the input Vertex in the work
     * data to the provided entity Vertex mapped from the document.
     * @param entityVertex the Vertex representing the mapped entity
     */
    void createHasEntityEdge(Vertex entityVertex);

    /**
     * Adds the target Vertex to the current workspace if necessary.
     * @param vertex the Vertex to add
     */
    void addVertexToWorkspaceIfNeeded(Vertex vertex);
}
