package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.sql.model.user.SqlUser;

import javax.persistence.*;
import java.io.Serializable;

@Embeddable
public class SqlWorkspaceUserId implements Serializable{
    private static final long serialVersionUID = 1L;
    @ManyToOne (cascade = CascadeType.ALL)
    private SqlUser user;
    @ManyToOne (cascade = CascadeType.ALL)
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
