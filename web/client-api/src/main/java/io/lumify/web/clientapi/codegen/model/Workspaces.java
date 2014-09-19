package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.Workspace;
public class Workspaces {
  private List<Workspace> workspaces = new ArrayList<Workspace>();
  public List<Workspace> getWorkspaces() {
    return workspaces;
  }
  public void setWorkspaces(List<Workspace> workspaces) {
    this.workspaces = workspaces;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Workspaces {\n");
    sb.append("  workspaces: ").append(workspaces).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

