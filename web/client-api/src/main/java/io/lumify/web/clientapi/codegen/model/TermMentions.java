package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.Element;
public class TermMentions {
  private List<Element> termMentions = new ArrayList<Element>();
  public List<Element> getTermMentions() {
    return termMentions;
  }
  public void setTermMentions(List<Element> termMentions) {
    this.termMentions = termMentions;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TermMentions {\n");
    sb.append("  termMentions: ").append(termMentions).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

