package io.lumify.web.clientapi.codegen.model;

import io.lumify.web.clientapi.codegen.model.TermMentionMetadata;
import io.lumify.web.clientapi.codegen.model.TermMentionKey;
public class TermMention {
  private TermMentionKey key = null;
  private TermMentionMetadata Metadata = null;
  public TermMentionKey getKey() {
    return key;
  }
  public void setKey(TermMentionKey key) {
    this.key = key;
  }

  @com.fasterxml.jackson.annotation.JsonProperty("Metadata")
public TermMentionMetadata getMetadata() {
    return Metadata;
  }
  @com.fasterxml.jackson.annotation.JsonProperty("Metadata")
public void setMetadata(TermMentionMetadata Metadata) {
    this.Metadata = Metadata;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class TermMention {\n");
    sb.append("  key: ").append(key).append("\n");
    sb.append("  Metadata: ").append(Metadata).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

