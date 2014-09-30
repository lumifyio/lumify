package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.DetectedObject;
public class DetectedObjects {
  private List<DetectedObject> detectedObjects = new ArrayList<DetectedObject>();
  public List<DetectedObject> getDetectedObjects() {
    return detectedObjects;
  }
  public void setDetectedObjects(List<DetectedObject> detectedObjects) {
    this.detectedObjects = detectedObjects;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class DetectedObjects {\n");
    sb.append("  detectedObjects: ").append(detectedObjects).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

