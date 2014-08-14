package io.lumify.sql.model.workspace;

import io.lumify.core.model.workspace.Workspace;
import io.lumify.sql.model.user.SqlUser;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "workspace")
public class SqlWorkspace implements Workspace {
    private String workspaceId;
    private String displayTitle;
    private SqlUser workspaceCreator;
    private List<SqlWorkspaceUser> sqlWorkspaceUserList = new ArrayList<SqlWorkspaceUser>();
    private List<SqlWorkspaceVertex> sqlWorkspaceVertices = new ArrayList<SqlWorkspaceVertex>();

    @Override
    @Id
    @Column(name = "workspace_id", unique = true)
    public String getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(String workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    @Column(name = "display_title")
    public String getDisplayTitle() {
        return displayTitle;
    }

    public void setDisplayTitle(String displayTitle) {
        this.displayTitle = displayTitle;
    }

    @OneToOne
    @JoinColumn(referencedColumnName = "user_id", name = "creator_user_id")
    public SqlUser getWorkspaceCreator() {
        return workspaceCreator;
    }

    public void setWorkspaceCreator(SqlUser workspaceCreator) {
        this.workspaceCreator = workspaceCreator;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "sqlWorkspaceUser.workspace", cascade = CascadeType.ALL)
    public List<SqlWorkspaceUser> getSqlWorkspaceUserList() {
        return sqlWorkspaceUserList;
    }

    public void setSqlWorkspaceUserList(List<SqlWorkspaceUser> sqlWorkspaceUserList) {
        this.sqlWorkspaceUserList = sqlWorkspaceUserList;
    }

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workspace", cascade = CascadeType.ALL)
    public List<SqlWorkspaceVertex> getSqlWorkspaceVertices() {
        return sqlWorkspaceVertices;
    }

    public void setSqlWorkspaceVertices(List<SqlWorkspaceVertex> sqlWorkspaceVertices) {
        this.sqlWorkspaceVertices = sqlWorkspaceVertices;
    }
}
