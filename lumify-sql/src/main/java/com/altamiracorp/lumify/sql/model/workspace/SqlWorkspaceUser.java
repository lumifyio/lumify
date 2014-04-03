package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.WorkspaceAccess;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name="workspace_user")
@AssociationOverrides({ @AssociationOverride (name="sqlWorkspaceUserId.user", joinColumns = @JoinColumn(name="user_id")),
    @AssociationOverride(name="sqlWorkspaceUserId.workspace", joinColumns = @JoinColumn(name="workspace_id"))})
public class SqlWorkspaceUser implements Serializable{
    private static final long serialVersionUID = 1L;
    @EmbeddedId
    private SqlWorkspaceUserId sqlWorkspaceUserId = new SqlWorkspaceUserId();
    @Column(name="access")
    private String workspaceAccess;

    public SqlWorkspaceUser() {};

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
}
