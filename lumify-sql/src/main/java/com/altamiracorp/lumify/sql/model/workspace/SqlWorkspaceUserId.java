package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.sql.model.user.SqlUser;

import javax.persistence.Embeddable;
import javax.persistence.ManyToOne;
import java.io.Serializable;

@Embeddable
public class SqlWorkspaceUserId {
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
}
