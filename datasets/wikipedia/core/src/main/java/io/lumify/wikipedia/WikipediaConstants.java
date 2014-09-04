package io.lumify.wikipedia;

import org.securegraph.Vertex;

public class WikipediaConstants {
    public static final String WIKIPEDIA_QUEUE = "wikipedia";
    public static final String CONFIG_FLUSH = "flush";
    public static final String WIKIPEDIA_PAGE_CONCEPT_URI = "http://lumify.io/wikipedia#wikipediaPage";
    public static final String WIKIPEDIA_PAGE_INTERNAL_LINK_WIKIPEDIA_PAGE_CONCEPT_URI = "http://lumify.io/wikipedia#wikipediaPageInternalLinkWikipediaPage";
    public static final String WIKIPEDIA_SOURCE = "Wikipedia";
    public static final String WIKIPEDIA_ID_PREFIX = "WIKIPEDIA_";
    public static final String WIKIPEDIA_LINK_ID_PREFIX = "WIKIPEDIA_LINK_";

    public static String getWikipediaPageVertexId(String pageTitle) {
        return WIKIPEDIA_ID_PREFIX + pageTitle.trim().toLowerCase();
    }

    public static String getWikipediaPageToPageEdgeId(Vertex pageVertex, Vertex linkedPageVertex) {
        return WIKIPEDIA_LINK_ID_PREFIX + getWikipediaPageTitleFromId(pageVertex.getId()) + "_" + getWikipediaPageTitleFromId(linkedPageVertex.getId());
    }

    public static String getWikipediaPageTitleFromId(Object id) {
        return id.toString().substring(WIKIPEDIA_ID_PREFIX.length());
    }
}
