package io.lumify.web.clientapi;

import io.lumify.web.clientapi.codegen.ApiException;
import io.lumify.web.clientapi.codegen.EdgeApi;
import io.lumify.web.clientapi.model.ClientApiEdgeWithVertexData;

public class EdgeApiExt extends EdgeApi {
    public ClientApiEdgeWithVertexData create(String sourceGraphVertexId, String destGraphVertexId, String label, String visibilitySource) throws ApiException {
        return create(sourceGraphVertexId, destGraphVertexId, label, visibilitySource, null, null);
    }

    public void setProperty(String edgeId, String propertyKey, String propertyName, String value, String visibilitySource, String justificationText) throws ApiException {
        setProperty(edgeId, propertyKey, propertyName, value, visibilitySource, justificationText, null, null);
    }
}
