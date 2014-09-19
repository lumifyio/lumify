package io.lumify.web.clientapi.codegen.model;

public class WorkspaceDiffItem {
  private String type = null;
  private String sandboxStatus = null;
  //public enum sandboxStatusEnum { PUBLIC, PUBLIC_CHANGED, PRIVATE, }; 
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  public String getSandboxStatus() {
    return sandboxStatus;
  }
  public void setSandboxStatus(String sandboxStatus) {
    this.sandboxStatus = sandboxStatus;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceDiffItem {\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  sandboxStatus: ").append(sandboxStatus).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

