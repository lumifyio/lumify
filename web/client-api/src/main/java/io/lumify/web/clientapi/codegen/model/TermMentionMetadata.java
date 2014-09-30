package io.lumify.web.clientapi.codegen.model;

public class TermMentionMetadata {
  private String sign = null;
  private String analyticProcess = null;
  private String conceptGraphVertexId = null;
  private String ontologyClassUri = null;
  private String graphVertexId = null;
  private String edgeId = null;
  public String getSign() {
    return sign;
  }
  public void setSign(String sign) {
    this.sign = sign;
  }

  public String getAnalyticProcess() {
    return analyticProcess;
  }
  public void setAnalyticProcess(String analyticProcess) {
    this.analyticProcess = analyticProcess;
  }

  public String getConceptGraphVertexId() {
    return conceptGraphVertexId;
  }
  public void setConceptGraphVertexId(String conceptGraphVertexId) {
    this.conceptGraphVertexId = conceptGraphVertexId;
  }

  public String getOntologyClassUri() {
    return ontologyClassUri;
  }
  public void setOntologyClassUri(String ontologyClassUri) {
    this.ontologyClassUri = ontologyClassUri;
  }

  public String getGraphVertexId() {
    return graphVertexId;
  }
  public void setGraphVertexId(String graphVertexId) {
    this.graphVertexId = graphVertexId;
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
    sb.append("class TermMentionMetadata {\n");
    sb.append("  sign: ").append(sign).append("\n");
    sb.append("  analyticProcess: ").append(analyticProcess).append("\n");
    sb.append("  conceptGraphVertexId: ").append(conceptGraphVertexId).append("\n");
    sb.append("  ontologyClassUri: ").append(ontologyClassUri).append("\n");
    sb.append("  graphVertexId: ").append(graphVertexId).append("\n");
    sb.append("  edgeId: ").append(edgeId).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

