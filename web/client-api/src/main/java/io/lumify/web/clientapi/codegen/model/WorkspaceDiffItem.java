package io.lumify.web.clientapi.codegen.model;

public class WorkspaceDiffItem {
  private String type = null;
  //public enum typeEnum { VertexDiffItem, PropertyDiffItem, EdgeDiffItem, }; 
  private String sandboxStatus = null;
  //public enum sandboxStatusEnum { PUBLIC, PUBLIC_CHANGED, PRIVATE, }; 
  private String vertexId = null;
  private String title = null;
  private Boolean visible = null;
  private String elementId = null;
  private String name = null;
  private String key = null;
  private String edgeId = null;
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

  public String getVertexId() {
    return vertexId;
  }
  public void setVertexId(String vertexId) {
    this.vertexId = vertexId;
  }

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  public Boolean getVisible() {
    return visible;
  }
  public void setVisible(Boolean visible) {
    this.visible = visible;
  }

  public String getElementId() {
    return elementId;
  }
  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }

  public String getEdgeId() {
    return edgeId;
  }
  public void setEdgeId(String edgeId) {
    this.edgeId = edgeId;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceDiffItem {\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  sandboxStatus: ").append(sandboxStatus).append("\n");
    sb.append("  vertexId: ").append(vertexId).append("\n");
    sb.append("  title: ").append(title).append("\n");
    sb.append("  visible: ").append(visible).append("\n");
    sb.append("  elementId: ").append(elementId).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("  key: ").append(key).append("\n");
    sb.append("  edgeId: ").append(edgeId).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

