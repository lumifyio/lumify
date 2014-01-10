package com.altamiracorp.lumify.core.ingest;

import com.altamiracorp.securegraph.Property;
import com.altamiracorp.securegraph.Vertex;
import org.json.JSONException;
import org.json.JSONObject;

public class ArtifactDetectedObject {
    private String concept;
    private String graphVertexId;
    private String rowKey;
    private Vertex resolvedVertex;
    private String x1;
    private String y1;
    private String x2;
    private String y2;

    public ArtifactDetectedObject(String x1, String y1, String x2, String y2, String concept) {
        setX1(x1);
        setY1(y1);
        setX2(x2);
        setY2(y2);
        this.concept = concept;
    }

    public String getX1() {
        return x1;
    }

    public void setX1(String x1) {
        this.x1 = x1;
    }

    public String getY1() {
        return y1;
    }

    public void setY1(String y1) {
        this.y1 = y1;
    }

    public String getX2() {
        return x2;
    }

    public void setX2(String x2) {
        this.x2 = x2;
    }

    public String getY2() {
        return y2;
    }

    public void setY2(String y2) {
        this.y2 = y2;
    }

    public String getConcept() {
        return concept;
    }

    public void setConcept(String concept) {
        this.concept = concept;
    }

    public String getGraphVertexId() {
        return graphVertexId;
    }

    public void setGraphVertexId(String graphVertexId) {
        this.graphVertexId = graphVertexId;
    }

    public String getRowKey() {
        return rowKey;
    }

    public void setRowKey(String rowKey) {
        this.rowKey = rowKey;
    }

    public Vertex getResolvedVertex() {
        return resolvedVertex;
    }

    public void setResolvedVertex(Vertex resolvedVertex) {
        this.resolvedVertex = resolvedVertex;
    }

    public JSONObject getJson() {
        try {
            JSONObject json = new JSONObject();
            if (resolvedVertex != null && resolvedVertex.getId() != null) {
                Vertex vertex = getResolvedVertex();
                json.put("graphVertexId", resolvedVertex.getId());
                for (Property property : vertex.getProperties()) {
                    // TODO handle multi-valued properties
                    json.put(property.getName(), property.getValue());
                }
            }
            json.put("concept", getConcept());
            json.put("_rowKey", getRowKey());
            json.put("x1", getX1());
            json.put("y1", getY1());
            json.put("x2", getX2());
            json.put("y2", getY2());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
