package io.lumify.web.clientapi.codegen.model;

import io.lumify.web.clientapi.codegen.model.GraphPosition;
public class WorkspaceVertex {
  private String vertexId = null;
  private GraphPosition graphPosition = null;
  public String getVertexId() {
    return vertexId;
  }
  public void setVertexId(String vertexId) {
    this.vertexId = vertexId;
  }

  public GraphPosition getGraphPosition() {
    return graphPosition;
  }
  public void setGraphPosition(GraphPosition graphPosition) {
    this.graphPosition = graphPosition;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceVertex {\n");
    sb.append("  vertexId: ").append(vertexId).append("\n");
    sb.append("  graphPosition: ").append(graphPosition).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

