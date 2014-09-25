package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.Property;
public class Element {
  private String id = null;
  private List<Property> properties = new ArrayList<Property>();
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public List<Property> getProperties() {
    return properties;
  }
  public void setProperties(List<Property> properties) {
    this.properties = properties;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Element {\n");
    sb.append("  id: ").append(id).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

