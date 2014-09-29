package io.lumify.web.clientapi.codegen.model;

import io.lumify.web.clientapi.codegen.model.DetectedObjectValue;
public class DetectedObject {
  private String key = null;
  private DetectedObjectValue value = null;
  public String getKey() {
    return key;
  }
  public void setKey(String key) {
    this.key = key;
  }

  public DetectedObjectValue getValue() {
    return value;
  }
  public void setValue(DetectedObjectValue value) {
    this.value = value;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DetectedObject {\n");
    sb.append("  key: ").append(key).append("\n");
    sb.append("  value: ").append(value).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

