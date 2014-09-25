package io.lumify.web.clientapi.codegen.model;

import java.util.*;
public class ArtifactImportResponse {
  private List<String> vertexIds = new ArrayList<String>();
  public List<String> getVertexIds() {
    return vertexIds;
  }
  public void setVertexIds(List<String> vertexIds) {
    this.vertexIds = vertexIds;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArtifactImportResponse {\n");
    sb.append("  vertexIds: ").append(vertexIds).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

