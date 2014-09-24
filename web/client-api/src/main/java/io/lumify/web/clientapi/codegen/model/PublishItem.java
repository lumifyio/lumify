package io.lumify.web.clientapi.codegen.model;

public class PublishItem {
  private String type = null;
  //public enum typeEnum { vertex, relationship, property, }; 
  /* 'delete' or '' */
  private String action = null;
  /* required for vertex publish */
  private String vertexId = null;
  /* required for edge publish */
  private String edgeId = null;
  /* required for property publish */
  private String elementId = null;
  /* required for edge publish */
  private String sourceId = null;
  /* required for edge publish */
  private String destId = null;
  /* required for property publish */
  private String key = null;
  /* required for property publish */
  private String name = null;
  private String error_msg = null;
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }

  public String getAction() {
    return action;
  }
  public void setAction(String action) {
    this.action = action;
  }

  public String getVertexId() {
    return vertexId;
  }
  public void setVertexId(String vertexId) {
    this.vertexId = vertexId;
  }

  public String getEdgeId() {
    return edgeId;
  }
  public void setEdgeId(String edgeId) {
    this.edgeId = edgeId;
  }

  public String getElementId() {
    return elementId;
  }
  public void setElementId(String elementId) {
    this.elementId = elementId;
  }

  public String getSourceId() {
    return sourceId;
  }
  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public String getDestId() {
    return destId;
  }
  public void setDestId(String destId) {
    this.destId = destId;
  }

  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public String getError_msg() {
    return error_msg;
  }
  public void setError_msg(String error_msg) {
    this.error_msg = error_msg;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublishItem {\n");
    sb.append("  type: ").append(type).append("\n");
    sb.append("  action: ").append(action).append("\n");
    sb.append("  vertexId: ").append(vertexId).append("\n");
    sb.append("  edgeId: ").append(edgeId).append("\n");
    sb.append("  elementId: ").append(elementId).append("\n");
    sb.append("  sourceId: ").append(sourceId).append("\n");
    sb.append("  destId: ").append(destId).append("\n");
    sb.append("  key: ").append(key).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("  error_msg: ").append(error_msg).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

