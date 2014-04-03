package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.sql.model.user.SqlUser;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "workspace")

public class SqlWorkspace implements Workspace {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name="workspace_id")
    private int id;
    @Column(name = "display_title")
    private String displayTitle;
    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private SqlUser creator;
    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn
    private SqlUser usersCurrentWorkspace;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sqlWorkspaceUserId.workspace")
    public Set<SqlWorkspaceUser> sqlWorkspaceUser = new HashSet<SqlWorkspaceUser>(0);

    public String getId() {
        return Integer.toString(id);
    }

    @Override
    public String getCreatorUserId() {
        return creator.getUserId();
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    public SqlUser getCreator() {
        return creator;
    }

    public void setCreator(SqlUser creator) {
        this.creator = creator;
    }

    public SqlUser getUsersCurrentWorkspace() {
        return usersCurrentWorkspace;
    }

    public void setUsersCurrentWorkspace(SqlUser usersCurrentWorkspace) {
        this.usersCurrentWorkspace = usersCurrentWorkspace;
    }

    public Set<SqlWorkspaceUser> getSqlWorkspaceUser() {
        return sqlWorkspaceUser;
    }

    public void setSqlWorkspaceUser(Set<SqlWorkspaceUser> sqlWorkspaceUser) {
        this.sqlWorkspaceUser = sqlWorkspaceUser;
    }

    @Override
    @Transient
    public JSONObject toJson(boolean includeVertices) {
        return null;
    }

    @Override
    public boolean hasWritePermissions(String userId) {
        return false;
    }
}
