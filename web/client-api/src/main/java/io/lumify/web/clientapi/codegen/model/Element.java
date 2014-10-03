package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.Property;
public class Element {
  private String id = null;
  private String sandboxStatus = null;
  private String visibilitySource = null;
  private List<Property> properties = new ArrayList<Property>();
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public String getSandboxStatus() {
    return sandboxStatus;
  }
  public void setSandboxStatus(String sandboxStatus) {
    this.sandboxStatus = sandboxStatus;
  }

  public String getVisibilitySource() {
    return visibilitySource;
  }
  public void setVisibilitySource(String visibilitySource) {
    this.visibilitySource = visibilitySource;
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
    sb.append("  sandboxStatus: ").append(sandboxStatus).append("\n");
    sb.append("  visibilitySource: ").append(visibilitySource).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

