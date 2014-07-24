package io.lumify.sql.model.workspace;

import io.lumify.sql.model.user.SqlUser;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
public class SqlWorkspaceUserId implements Serializable{
    private static final long serialVersionUID = 1L;

    @ManyToOne
    private SqlUser user;

    @ManyToOne
    private SqlWorkspace workspace;

    public SqlUser getUser() {
        return user;
    }

    public void setUser(SqlUser user) {
        this.user = user;
    }

    public SqlWorkspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(SqlWorkspace sqlWorkspace) {
        this.workspace = sqlWorkspace;
    }

    public boolean equals (Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SqlWorkspaceUserId sqlWorkspaceUserId = (SqlWorkspaceUserId) o;
        if (user != null ? !user.equals(sqlWorkspaceUserId.user) : sqlWorkspaceUserId.user != null) {
            return false;
        }

        if (workspace != null ? !workspace.equals(sqlWorkspaceUserId.workspace) : sqlWorkspaceUserId.workspace != null) {
            return false;
        }
        return true;
    }

    public int hashCode () {
        int result;
        result = (user != null ? user.hashCode() : 0);
        result = 31 * result + (workspace != null ? workspace.hashCode() : 0);
        return result;
    }
}
