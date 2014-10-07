package io.lumify.sql.model.workspace;

import io.lumify.web.clientapi.model.WorkspaceAccess;
import io.lumify.sql.model.user.SqlUser;

import javax.persistence.*;

@Entity
@Table(name = "workspace_user")
@AssociationOverrides({@AssociationOverride(name = "sqlWorkspaceUser.user", joinColumns = @JoinColumn(name = "user_id")),
        @AssociationOverride(name = "sqlWorkspaceUser.workspace", joinColumns = @JoinColumn(name = "workspace_id"))})
public class SqlWorkspaceUser {
    @EmbeddedId
    private SqlWorkspaceUserId sqlWorkspaceUser = new SqlWorkspaceUserId();
    @Column(name = "access")
    private String workspaceAccess;

    public SqlWorkspaceUser() {
    }

    public SqlWorkspaceUserId getSqlWorkspaceUser() {
        return sqlWorkspaceUser;
    }

    public void setSqlWorkspaceUser(SqlWorkspaceUserId sqlWorkspaceUser) {
        this.sqlWorkspaceUser = sqlWorkspaceUser;
    }

    public String getWorkspaceAccess() {
        return workspaceAccess;
    }

    public void setWorkspaceAccess(WorkspaceAccess workspaceAccess) {
        this.workspaceAccess = workspaceAccess.name();
    }

    @Transient
    public SqlWorkspace getWorkspace() {
        return getSqlWorkspaceUser().getWorkspace();
    }

    public void setWorkspace(SqlWorkspace workspace) {
        getSqlWorkspaceUser().setWorkspace(workspace);
    }

    @Transient
    public SqlUser getUser() {
        return getSqlWorkspaceUser().getUser();
    }

    public void setUser(SqlUser user) {
        getSqlWorkspaceUser().setUser(user);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SqlWorkspaceUser sqlWorkspaceUser = (SqlWorkspaceUser) o;
        if (getSqlWorkspaceUser() != null ? !getSqlWorkspaceUser().equals(sqlWorkspaceUser.getSqlWorkspaceUser()) : sqlWorkspaceUser.getSqlWorkspaceUser() != null) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        return (getSqlWorkspaceUser() != null ? getSqlWorkspaceUser().hashCode() : 0);
    }
}
