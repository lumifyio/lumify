package io.lumify.sql.model.workspace;

import io.lumify.core.model.workspace.Workspace;
import io.lumify.sql.model.user.SqlUser;

import javax.persistence.*;

@Entity
@Table(name = "workspace")
public class SqlWorkspace implements Workspace {
    private int workspaceId;
    private String displayTitle;
    private SqlUser workspaceCreator;

    @Override
    @Transient
    public String getId() {
        return Integer.toString(workspaceId);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "workspace_id", unique = true)
    public int getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(int workspaceId) {
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
    public SqlUser getWorkspaceCreator () {
        return workspaceCreator;
    }

    public void setWorkspaceCreator (SqlUser workspaceCreator) {
        this.workspaceCreator = workspaceCreator;
    }
}
