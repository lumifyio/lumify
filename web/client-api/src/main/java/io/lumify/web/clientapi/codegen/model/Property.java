package io.lumify.web.clientapi.codegen.model;


public class Property {
  private String key = null;
  private String name = null;
  private Boolean streamingPropertyValue = null;
  private Object value = null;
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }

  public Boolean getStreamingPropertyValue() {
    return streamingPropertyValue;
  }
  public void setStreamingPropertyValue(Boolean streamingPropertyValue) {
    this.streamingPropertyValue = streamingPropertyValue;
  }

  public Object getValue() {
    return value;
  }
  public void setValue(Object value) {
    this.value = value;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Property {\n");
    sb.append("  key: ").append(key).append("\n");
    sb.append("  name: ").append(name).append("\n");
    sb.append("  streamingPropertyValue: ").append(streamingPropertyValue).append("\n");
    sb.append("  value: ").append(value).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

