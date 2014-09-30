package io.lumify.web.clientapi.codegen.model;

public class DetectedObjectValue {
  private Double x1 = null;
  private Double x2 = null;
  private Double y1 = null;
  private Double y2 = null;
  private String concept = null;
  private String edgeId = null;
  private String resolvedVertexId = null;
  private String originalPropertyKey = null;
  private String process = null;
  public Double getX1() {
    return x1;
  }
  public void setX1(Double x1) {
    this.x1 = x1;
  }

  public Double getX2() {
    return x2;
  }
  public void setX2(Double x2) {
    this.x2 = x2;
  }

  public Double getY1() {
    return y1;
  }
  public void setY1(Double y1) {
    this.y1 = y1;
  }

  public Double getY2() {
    return y2;
  }
  public void setY2(Double y2) {
    this.y2 = y2;
  }

  public String getConcept() {
    return concept;
  }
  public void setConcept(String concept) {
    this.concept = concept;
  }

  public String getEdgeId() {
    return edgeId;
  }
  public void setEdgeId(String edgeId) {
    this.edgeId = edgeId;
  }

  public String getResolvedVertexId() {
    return resolvedVertexId;
  }
  public void setResolvedVertexId(String resolvedVertexId) {
    this.resolvedVertexId = resolvedVertexId;
  }

  public String getOriginalPropertyKey() {
    return originalPropertyKey;
  }
  public void setOriginalPropertyKey(String originalPropertyKey) {
    this.originalPropertyKey = originalPropertyKey;
  }

  public String getProcess() {
    return process;
  }
  public void setProcess(String process) {
    this.process = process;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DetectedObjectValue {\n");
    sb.append("  x1: ").append(x1).append("\n");
    sb.append("  x2: ").append(x2).append("\n");
    sb.append("  y1: ").append(y1).append("\n");
    sb.append("  y2: ").append(y2).append("\n");
    sb.append("  concept: ").append(concept).append("\n");
    sb.append("  edgeId: ").append(edgeId).append("\n");
    sb.append("  resolvedVertexId: ").append(resolvedVertexId).append("\n");
    sb.append("  originalPropertyKey: ").append(originalPropertyKey).append("\n");
    sb.append("  process: ").append(process).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

