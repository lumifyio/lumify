package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.OntologyConcept;
import io.lumify.web.clientapi.codegen.model.OntologyRelationship;
import io.lumify.web.clientapi.codegen.model.OntologyProperty;
public class Ontology {
  private List<OntologyProperty> properties = new ArrayList<OntologyProperty>();
  private List<OntologyRelationship> relationships = new ArrayList<OntologyRelationship>();
  private List<OntologyConcept> concepts = new ArrayList<OntologyConcept>();
  public List<OntologyProperty> getProperties() {
    return properties;
  }
  public void setProperties(List<OntologyProperty> properties) {
    this.properties = properties;
  }

  public List<OntologyRelationship> getRelationships() {
    return relationships;
  }
  public void setRelationships(List<OntologyRelationship> relationships) {
    this.relationships = relationships;
  }

  public List<OntologyConcept> getConcepts() {
    return concepts;
  }
  public void setConcepts(List<OntologyConcept> concepts) {
    this.concepts = concepts;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Ontology {\n");
    sb.append("  properties: ").append(properties).append("\n");
    sb.append("  relationships: ").append(relationships).append("\n");
    sb.append("  concepts: ").append(concepts).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

