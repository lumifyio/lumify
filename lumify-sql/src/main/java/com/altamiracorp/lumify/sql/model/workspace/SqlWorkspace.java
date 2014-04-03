package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.model.workspace.Workspace;
import com.altamiracorp.lumify.sql.model.user.SqlUser;
import org.hibernate.annotations.GenericGenerator;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Set;

@Entity
@Table(name = "workspace")
public class SqlWorkspace implements Workspace {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id")
    private int id;
    @Column(name="display_title")
    private String displayTitle;
    @OneToOne
    @PrimaryKeyJoinColumn
    private SqlUser creator;
    @OneToMany (mappedBy = "id")
    private Set<SqlUser> userWithWriteAccess;
    @OneToMany (mappedBy = "id")
    private Set<SqlUser> userWithReadAccess;

    public String getId () {
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

    public Set<SqlUser> getUserWithWriteAccess() {
        return userWithWriteAccess;
    }

    public void setUserWithWriteAccess(Set<SqlUser> userWithWriteAccess) {
        this.userWithWriteAccess = userWithWriteAccess;
    }

    public Set<SqlUser> getUserWithReadAccess() {
        return userWithReadAccess;
    }

    public void setUserWithReadAccess(Set<SqlUser> userWithReadAccess) {
        this.userWithReadAccess = userWithReadAccess;
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
