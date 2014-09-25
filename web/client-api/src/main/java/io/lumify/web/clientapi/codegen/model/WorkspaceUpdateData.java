package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.WorkspaceUserUpdate;
import io.lumify.web.clientapi.codegen.model.WorkspaceEntityUpdate;
public class WorkspaceUpdateData {
  private String title = null;
  private List<WorkspaceEntityUpdate> entityUpdates = new ArrayList<WorkspaceEntityUpdate>();
  private List<String> entityDeletes = new ArrayList<String>();
  private List<WorkspaceUserUpdate> userUpdates = new ArrayList<WorkspaceUserUpdate>();
  private List<String> userDeletes = new ArrayList<String>();
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  public List<WorkspaceEntityUpdate> getEntityUpdates() {
    return entityUpdates;
  }
  public void setEntityUpdates(List<WorkspaceEntityUpdate> entityUpdates) {
    this.entityUpdates = entityUpdates;
  }

  public List<String> getEntityDeletes() {
    return entityDeletes;
  }
  public void setEntityDeletes(List<String> entityDeletes) {
    this.entityDeletes = entityDeletes;
  }

  public List<WorkspaceUserUpdate> getUserUpdates() {
    return userUpdates;
  }
  public void setUserUpdates(List<WorkspaceUserUpdate> userUpdates) {
    this.userUpdates = userUpdates;
  }

  public List<String> getUserDeletes() {
    return userDeletes;
  }
  public void setUserDeletes(List<String> userDeletes) {
    this.userDeletes = userDeletes;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkspaceUpdateData {\n");
    sb.append("  title: ").append(title).append("\n");
    sb.append("  entityUpdates: ").append(entityUpdates).append("\n");
    sb.append("  entityDeletes: ").append(entityDeletes).append("\n");
    sb.append("  userUpdates: ").append(userUpdates).append("\n");
    sb.append("  userDeletes: ").append(userDeletes).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

