package com.altamiracorp.lumify.web.routes.graph;

import com.altamiracorp.lumify.core.model.graph.GraphVertex;
import com.altamiracorp.lumify.core.model.ontology.PropertyName;
import com.altamiracorp.lumify.core.model.ontology.VertexType;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.model.graph.GraphRepository;
import com.altamiracorp.lumify.core.model.ontology.Concept;
import com.altamiracorp.lumify.core.model.ontology.OntologyRepository;
import com.altamiracorp.lumify.web.BaseRequestHandler;
import com.altamiracorp.miniweb.HandlerChain;
import com.google.inject.Inject;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

public class GraphRelatedVertices extends BaseRequestHandler {
    private final GraphRepository graphRepository;
    private final OntologyRepository ontologyRepository;

    @Inject
    public GraphRelatedVertices(final OntologyRepository ontologyRepo, final GraphRepository graphRepo) {
        ontologyRepository = ontologyRepo;
        graphRepository = graphRepo;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, HandlerChain chain) throws Exception {
        String graphVertexId = getAttributeString(request, "graphVertexId");
        String limitParentConceptId = getOptionalParameter(request, "limitParentConceptId");

        User user = getUser(request);
        List<Concept> limitConcepts = null;

        if (limitParentConceptId != null) {
            limitConcepts = ontologyRepository.getConceptByIdAndChildren(limitParentConceptId, user);
            if (limitConcepts == null) {
                throw new RuntimeException("Bad 'limitParentConceptId', no concept found for id: " + limitParentConceptId);
            }
        }

        List<GraphVertex> graphVertices = graphRepository.getRelatedVertices(graphVertexId, user);

        JSONObject json = new JSONObject();
        JSONArray verticesJson = new JSONArray();
        for (GraphVertex graphVertex : graphVertices) {
            if (limitConcepts != null && isLimited(limitConcepts, graphVertex)) {
                continue;
            }
            JSONObject graphVertexJson = graphVertex.toJson();
            verticesJson.put(graphVertexJson);
        }
        json.put("vertices", verticesJson);

        respondWithJson(response, json);

        chain.next(request, response);
    }

    private boolean isLimited(List<Concept> limitConcepts, GraphVertex graphVertex) {
        String conceptId = (String) graphVertex.getProperty(PropertyName.CONCEPT_TYPE);
        for (Concept concept : limitConcepts) {
            if (concept.getId().equals(conceptId)) {
                return false;
            }
        }
        return true;
    }
}

