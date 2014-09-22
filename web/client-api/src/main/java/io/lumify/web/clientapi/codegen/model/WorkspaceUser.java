package io.lumify.web.clientapi.codegen.model;

public class WorkspaceUser {
  private String userId = null;
  private String access = null;
  public String getUserId() {
    return userId;
  }
  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getAccess() {
    return access;
  }
  public void setAccess(String access) {
    this.access = access;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceUser {\n");
    sb.append("  userId: ").append(userId).append("\n");
    sb.append("  access: ").append(access).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

