package io.lumify.web.clientapi.codegen.model;

public class Workspace {
  private String workspaceId = null;
  public String getWorkspaceId() {
    return workspaceId;
  }
  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Workspace {\n");
    sb.append("  workspaceId: ").append(workspaceId).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

