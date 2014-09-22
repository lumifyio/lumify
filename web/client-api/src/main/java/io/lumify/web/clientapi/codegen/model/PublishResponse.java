package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.PublishItem;
public class PublishResponse {
  private Boolean success = null;
  private List<PublishItem> failures = new ArrayList<PublishItem>();
  public Boolean getSuccess() {
    return success;
  }
  public void setSuccess(Boolean success) {
    this.success = success;
  }

  public List<PublishItem> getFailures() {
    return failures;
  }
  public void setFailures(List<PublishItem> failures) {
    this.failures = failures;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class PublishResponse {\n");
    sb.append("  success: ").append(success).append("\n");
    sb.append("  failures: ").append(failures).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

