package io.lumify.web.clientapi.codegen.model;

public class UserMe {
  private String id = null;
  private String status = null;
  //public enum statusEnum { ONLINE, OFFLINE, }; 
  private String csrfToken = null;
  private String currentWorkspaceId = null;
  private String userName = null;
  private String displayName = null;
  private String userType = null;
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }

  public String getStatus() {
    return status;
  }
  public void setStatus(String status) {
    this.status = status;
  }

  public String getCsrfToken() {
    return csrfToken;
  }
  public void setCsrfToken(String csrfToken) {
    this.csrfToken = csrfToken;
  }

  public String getCurrentWorkspaceId() {
    return currentWorkspaceId;
  }
  public void setCurrentWorkspaceId(String currentWorkspaceId) {
    this.currentWorkspaceId = currentWorkspaceId;
  }

  public String getUserName() {
    return userName;
  }
  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String getDisplayName() {
    return displayName;
  }
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getUserType() {
    return userType;
  }
  public void setUserType(String userType) {
    this.userType = userType;
  }

  @Override
  public String toString()  {
    StringBuilder sb = new StringBuilder();
    sb.append("class UserMe {\n");
    sb.append("  id: ").append(id).append("\n");
    sb.append("  status: ").append(status).append("\n");
    sb.append("  csrfToken: ").append(csrfToken).append("\n");
    sb.append("  currentWorkspaceId: ").append(currentWorkspaceId).append("\n");
    sb.append("  userName: ").append(userName).append("\n");
    sb.append("  displayName: ").append(displayName).append("\n");
    sb.append("  userType: ").append(userType).append("\n");
    sb.append("}\n");
    return sb.toString();
  }
}

