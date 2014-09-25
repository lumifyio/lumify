package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.GraphVertexSearchVertex;
public class GraphVertexSearchResult {
  private List<GraphVertexSearchVertex> vertices = new ArrayList<GraphVertexSearchVertex>();
  private Integer nextOffset = null;
  private Integer retrievalTime = null;
  private Integer totalTime = null;
  private Integer totalHits = null;
  private Integer searchTime = null;
  public List<GraphVertexSearchVertex> getVertices() {
    return vertices;
  }
  public void setVertices(List<GraphVertexSearchVertex> vertices) {
    this.vertices = vertices;
  }

  public Integer getNextOffset() {
    return nextOffset;
  }
  public void setNextOffset(Integer nextOffset) {
    this.nextOffset = nextOffset;
  }

  public Integer getRetrievalTime() {
    return retrievalTime;
  }
  public void setRetrievalTime(Integer retrievalTime) {
    this.retrievalTime = retrievalTime;
  }

  public Integer getTotalTime() {
    return totalTime;
  }
  public void setTotalTime(Integer totalTime) {
    this.totalTime = totalTime;
  }

  public Integer getTotalHits() {
    return totalHits;
  }
  public void setTotalHits(Integer totalHits) {
    this.totalHits = totalHits;
  }

  public Integer getSearchTime() {
    return searchTime;
  }
  public void setSearchTime(Integer searchTime) {
    this.searchTime = searchTime;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GraphVertexSearchResult {\n");
    sb.append("  vertices: ").append(vertices).append("\n");
    sb.append("  nextOffset: ").append(nextOffset).append("\n");
    sb.append("  retrievalTime: ").append(retrievalTime).append("\n");
    sb.append("  totalTime: ").append(totalTime).append("\n");
    sb.append("  totalHits: ").append(totalHits).append("\n");
    sb.append("  searchTime: ").append(searchTime).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

