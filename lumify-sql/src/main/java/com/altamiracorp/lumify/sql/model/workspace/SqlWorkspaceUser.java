package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.WorkspaceAccess;
import com.altamiracorp.lumify.sql.model.user.SqlUser;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "workspace_user")
@AssociationOverrides({@AssociationOverride(name = "sqlWorkspaceUserId.user", joinColumns = @JoinColumn(name = "user_id")),
        @AssociationOverride(name = "sqlWorkspaceUserId.workspace", joinColumns = @JoinColumn(name = "workspace_id"))})
public class SqlWorkspaceUser implements Serializable{
    public static final long serialVersionUID = 1L;
    @EmbeddedId
    private SqlWorkspaceUserId sqlWorkspaceUserId = new SqlWorkspaceUserId();
    @Column(name = "access")
    private String workspaceAccess;

    public SqlWorkspaceUser() {
    }

    public SqlWorkspaceUserId getSqlWorkspaceUserId() {
        return sqlWorkspaceUserId;
    }

    public void setSqlWorkspaceUserId(SqlWorkspaceUserId sqlWorkspaceUserId) {
        this.sqlWorkspaceUserId = sqlWorkspaceUserId;
    }

    public String getWorkspaceAccess() {
        return workspaceAccess;
    }

    public void setWorkspaceAccess(WorkspaceAccess workspaceAccess) {
        this.workspaceAccess = workspaceAccess.name();
    }

    @Transient
    public SqlWorkspace getWorkspace() {
        return getSqlWorkspaceUserId().getWorkspace();
    }

    public void setWorkspace(SqlWorkspace workspace) {
        getSqlWorkspaceUserId().setWorkspace(workspace);
    }

    @Transient
    public SqlUser getUser() {
        return getSqlWorkspaceUserId().getUser();
    }

    public void setUser(SqlUser user) {
        getSqlWorkspaceUserId().setUser(user);
    }

    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SqlWorkspaceUser sqlWorkspaceUser = (SqlWorkspaceUser) o;
        if (getSqlWorkspaceUserId() != null ? !getSqlWorkspaceUserId().equals(sqlWorkspaceUser.getSqlWorkspaceUserId()) : sqlWorkspaceUser.getSqlWorkspaceUserId() != null) {
            return false;
        }
        return true;
    }

    public int hashCode () {
        return (getSqlWorkspaceUserId() != null ? getSqlWorkspaceUserId().hashCode() : 0);
    }
}
