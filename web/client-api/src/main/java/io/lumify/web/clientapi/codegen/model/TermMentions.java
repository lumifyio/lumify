package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.TermMention;
public class TermMentions {
  private List<TermMention> termMentions = new ArrayList<TermMention>();
  public List<TermMention> getTermMentions() {
    return termMentions;
  }
  public void setTermMentions(List<TermMention> termMentions) {
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

