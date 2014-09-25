package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import java.util.Map;
public class GraphVertexSearchVertex {
  private String id = null;
  private String sandboxStatus = null;
  private List<String> edgeLabels = new ArrayList<String>();
  private List<Map<String,Object>> properties = new ArrayList<Map<String,Object>>();
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public String getSandboxStatus() {
    return sandboxStatus;
  }
  public void setSandboxStatus(String sandboxStatus) {
    this.sandboxStatus = sandboxStatus;
  }

  public List<String> getEdgeLabels() {
    return edgeLabels;
  }
  public void setEdgeLabels(List<String> edgeLabels) {
    this.edgeLabels = edgeLabels;
  }

  public List<Map<String,Object>> getProperties() {
    return properties;
  }
  public void setProperties(List<Map<String,Object>> properties) {
    this.properties = properties;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GraphVertexSearchVertex {\n");
    sb.append("  id: ").append(id).append("\n");
    sb.append("  sandboxStatus: ").append(sandboxStatus).append("\n");
    sb.append("  edgeLabels: ").append(edgeLabels).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

