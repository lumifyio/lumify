package io.lumify.web.clientapi.codegen.model;

import java.util.*;
import io.lumify.web.clientapi.codegen.model.WorkspaceUser;
import io.lumify.web.clientapi.codegen.model.WorkspaceVertex;
public class Workspace {
  private String workspaceId = null;
  private String title = null;
  private String createdBy = null;
  private Boolean isSharedToUser = null;
  private Boolean isEditable = null;
  private List<WorkspaceUser> users = new ArrayList<WorkspaceUser>();
  private List<WorkspaceVertex> vertices = new ArrayList<WorkspaceVertex>();
  public String getWorkspaceId() {
    return workspaceId;
  }
  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }

  public String getCreatedBy() {
    return createdBy;
  }
  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public Boolean getIsSharedToUser() {
    return isSharedToUser;
  }
  public void setIsSharedToUser(Boolean isSharedToUser) {
    this.isSharedToUser = isSharedToUser;
  }

  public Boolean getIsEditable() {
    return isEditable;
  }
  public void setIsEditable(Boolean isEditable) {
    this.isEditable = isEditable;
  }

  public List<WorkspaceUser> getUsers() {
    return users;
  }
  public void setUsers(List<WorkspaceUser> users) {
    this.users = users;
  }

  public List<WorkspaceVertex> getVertices() {
    return vertices;
  }
  public void setVertices(List<WorkspaceVertex> vertices) {
    this.vertices = vertices;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class Workspace {\n");
    sb.append("  workspaceId: ").append(workspaceId).append("\n");
    sb.append("  title: ").append(title).append("\n");
    sb.append("  createdBy: ").append(createdBy).append("\n");
    sb.append("  isSharedToUser: ").append(isSharedToUser).append("\n");
    sb.append("  isEditable: ").append(isEditable).append("\n");
    sb.append("  users: ").append(users).append("\n");
    sb.append("  vertices: ").append(vertices).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

