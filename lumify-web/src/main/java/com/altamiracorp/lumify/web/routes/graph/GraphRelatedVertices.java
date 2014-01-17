package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.core.util.GraphUtil;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.altamiracorp.securegraph.Direction;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Iterator;
import java.util.List;

public class GraphRelatedVertices extends BaseRequestHandler {
    private final Graph graph;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphRelatedVertices(final OntologyRepository ontologyRepo, final Graph graph) {
        ontologyRepository = ontologyRepo;
        this.graph = graph;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getAttributeString(request, "graphVertexId");
        String limitParentConceptId = getOptionalParameter(request, "limitParentConceptId");

        User user = getUser(request);
        List<Concept> limitConcepts = null;

        if (limitParentConceptId != null) {
            limitConcepts = ontologyRepository.getConceptByIdAndChildren(limitParentConceptId);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " + limitParentConceptId);
            }
        }

        Iterator<Vertex> verticesIterator = graph.getVertex(graphVertexId, user.getAuthorizations())
                .getVertices(Direction.BOTH, user.getAuthorizations()).iterator();

        JSONObject json = new JSONObject();
        JSONArray verticesJson = new JSONArray();
        while (verticesIterator.hasNext()) {
            Vertex vertex = verticesIterator.next();
            if (limitConcepts != null && isLimited(limitConcepts, vertex)) {
                continue;
            }
            verticesJson.put(GraphUtil.toJson(vertex));
        }
        json.put("vertices", verticesJson);

        respondWithJson(response, json);

        chain.next(request, response);
    }

    private boolean isLimited(List<Concept> limitConcepts, Vertex vertex) {
        String conceptId = (String) vertex.getPropertyValue(PropertyName.CONCEPT_TYPE.toString(), 0);
        for (Concept concept : limitConcepts) {
            if (concept.getId().equals(conceptId)) {
                return false;
            }
        }
        return true;
    }
}

