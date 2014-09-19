package io.lumify.web.clientapi.codegen.model;

import java.util.*;
public class OntologyConcept {
  private String id = null;
  private String title = null;
  private String displayType = null;
  private String pluralDisplayName = null;
  private String parentConcept = null;
  private String displayName = null;
  private List<String> properties = new ArrayList<String>();
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  public String getDisplayType() {
    return displayType;
  }
  public void setDisplayType(String displayType) {
    this.displayType = displayType;
  }

  public String getPluralDisplayName() {
    return pluralDisplayName;
  }
  public void setPluralDisplayName(String pluralDisplayName) {
    this.pluralDisplayName = pluralDisplayName;
  }

  public String getParentConcept() {
    return parentConcept;
  }
  public void setParentConcept(String parentConcept) {
    this.parentConcept = parentConcept;
  }

  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public List<String> getProperties() {
    return properties;
  }
  public void setProperties(List<String> properties) {
    this.properties = properties;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class OntologyConcept {\n");
    sb.append("  id: ").append(id).append("\n");
    sb.append("  title: ").append(title).append("\n");
    sb.append("  displayType: ").append(displayType).append("\n");
    sb.append("  pluralDisplayName: ").append(pluralDisplayName).append("\n");
    sb.append("  parentConcept: ").append(parentConcept).append("\n");
    sb.append("  displayName: ").append(displayName).append("\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

