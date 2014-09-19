package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.WorkspaceDiffItem;
public class WorkspaceDiff {
  private List<WorkspaceDiffItem> diffs = new ArrayList<WorkspaceDiffItem>();
  public List<WorkspaceDiffItem> getDiffs() {
    return diffs;
  }
  public void setDiffs(List<WorkspaceDiffItem> diffs) {
    this.diffs = diffs;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceDiff {\n");
    sb.append("  diffs: ").append(diffs).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

