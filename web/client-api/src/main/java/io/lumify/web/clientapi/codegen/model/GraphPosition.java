package io.lumify.web.clientapi.codegen.model;

public class GraphPosition {
  private Integer x = null;
  private Integer y = null;
  public Integer getX() {
    return x;
  }
  public void setX(Integer x) {
    this.x = x;
  }

  public Integer getY() {
    return y;
  }
  public void setY(Integer y) {
    this.y = y;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class GraphPosition {\n");
    sb.append("  x: ").append(x).append("\n");
    sb.append("  y: ").append(y).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

