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
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "workspace_id", unique = true)
    private int workspaceId;
    @Column(name = "display_title")
    private String displayTitle;
    @OneToOne(fetch = FetchType.LAZY)
    @PrimaryKeyJoinColumn (name="user_id")
    private SqlUser creator;
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "sqlWorkspaceUserId.workspace")
    public Set<SqlWorkspaceUser> sqlWorkspaceUser = new HashSet<SqlWorkspaceUser>(0);
    @OneToMany (fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "sqlWorkspaceVertexId.workspace")
    private Set<SqlWorkspaceVertex> sqlWorkspaceVertices = new HashSet<SqlWorkspaceVertex>(0);

    public String getId() {
        return Integer.toString(workspaceId);
    }

    public void setId(int workspaceId) {
        this.workspaceId = workspaceId;
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

    public Set<SqlWorkspaceUser> getSqlWorkspaceUser() {
        return sqlWorkspaceUser;
    }

    public void setSqlWorkspaceUser(Set<SqlWorkspaceUser> sqlWorkspaceUser) {
        this.sqlWorkspaceUser = sqlWorkspaceUser;
    }

    public Set<SqlWorkspaceVertex> getSqlWorkspaceVertices() {
        return sqlWorkspaceVertices;
    }

    public void setSqlWorkspaceVertices(Set<SqlWorkspaceVertex> sqlWorkspaceVertices) {
        this.sqlWorkspaceVertices = sqlWorkspaceVertices;
    }
}
