package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.workspace.*;
import com.altamiracorp.lumify.core.model.workspace.diff.DiffItem;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.sql.model.user.SqlUser;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.inject.Inject;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqlWorkspaceRepository implements WorkspaceRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlWorkspaceRepository.class);
    private SessionFactory sessionFactory;

    @Override
    public void init(Map map) {

    }

    @Override
    public void delete(Workspace workspace, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionFactory.getCurrentSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            session.delete(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public Workspace findById(String workspaceId, User user) {
        Session session = sessionFactory.openSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).add(Restrictions.eq("workspaceId", Integer.parseInt(workspaceId))).list();
        if (workspaces.size() == 0) {
            return null;
        } else if (workspaces.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlWorkspace) workspaces.get(0);
        }
    }

    @Override
    public Workspace add(String title, User user) {
        Session session = sessionFactory.getCurrentSession();

        Transaction transaction = null;
        SqlWorkspace newWorkspace;
        try {
            transaction = session.beginTransaction();
            newWorkspace = new SqlWorkspace();
            newWorkspace.setDisplayTitle(title);
            newWorkspace.setCreator((SqlUser) user);

            SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
            sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);
            sqlWorkspaceUser.setUser((SqlUser) user);
            sqlWorkspaceUser.setWorkspace(newWorkspace);

            LOGGER.debug("add %s to workspace table", title);
            newWorkspace.getSqlWorkspaceUser().add(sqlWorkspaceUser);
            session.save(newWorkspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
        return newWorkspace;
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        Session session = sessionFactory.openSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).list();
        session.close();
        return new ConvertingIterable<Object, Workspace>(workspaces) {
            @Override
            protected Workspace convert(Object obj) {
                return (SqlWorkspace) obj;
            }
        };
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionFactory.getCurrentSession();

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            ((SqlWorkspace) workspace).setDisplayTitle(title);
            session.update(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user) {
        if (!doesUserHaveWriteAccess(workspace, user) && !doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Set<SqlWorkspaceUser> sqlWorkspaceUsers = ((SqlWorkspace) workspace).getSqlWorkspaceUser();
        List<WorkspaceUser> withAccess = new ArrayList<WorkspaceUser>();

        for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
            if (!sqlWorkspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.NONE.toString())) {
                String userId = sqlWorkspaceUser.getUser().getUserId();
                boolean isCreator = ((SqlWorkspace) workspace).getCreator().getUserId().equals(userId);
                withAccess.add(new WorkspaceUser(userId, WorkspaceAccess.valueOf(sqlWorkspaceUser.getWorkspaceAccess()), isCreator));
            }
        }

        return withAccess;
    }

    @Override
    public List<WorkspaceEntity> findEntities(String workspaceId, User user) {
        return new ArrayList<WorkspaceEntity>();
    }

    @Override
    public List<WorkspaceEntity> findEntities(Workspace workspace, User user) {
        return new ArrayList<WorkspaceEntity>();
    }

    @Override
    public Workspace copy(Workspace workspace, User user) {
        throw new RuntimeException("Not yet Implemented");
    }

    @Override
    public void deleteEntityFromWorkspace(Workspace workspace, Object vertexId, User user) {
        // TODO this should probably be implemented
        LOGGER.warn("deleteEntityFromWorkspace not implemented");
    }

    @Override
    public void updateEntityOnWorkspace(Workspace workspace, Object vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user) {
        // TODO this should probably be implemented
        LOGGER.warn("updateEntityOnWorkspace not implemented");
    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        updateUserOnWorkspace(workspace, userId, WorkspaceAccess.NONE, user);
    }

    private boolean doesUserHaveWriteAccess(Workspace workspace, User user) {
        Set<SqlWorkspaceUser> sqlWorkspaceUsers = ((SqlWorkspace) workspace).getSqlWorkspaceUser();
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) && workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.WRITE.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean doesUserHaveReadAccess(Workspace workspace, User user) {
        if (doesUserHaveWriteAccess(workspace, user)) {
            return true;
        }
        Set<SqlWorkspaceUser> sqlWorkspaceUsers = ((SqlWorkspace) workspace).getSqlWorkspaceUser();
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) &&
                    workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.READ.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param workspace       workspace to update
     * @param userId          userId of the user you want to update
     * @param workspaceAccess level of access to set
     * @param user            user requesting the update
     */
    @Override
    public void updateUserOnWorkspace(Workspace workspace, String userId, WorkspaceAccess workspaceAccess, User user) {
        checkNotNull(userId);
        if (!doesUserHaveWriteAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionFactory.getCurrentSession();

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            SqlWorkspace sqlWorkspace = (SqlWorkspace) workspace;
            Set<SqlWorkspaceUser> sqlWorkspaceUsers = sqlWorkspace.getSqlWorkspaceUser();
            for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
                if (sqlWorkspaceUser.getUser().getUserId().equals(userId)) {
                    if (WorkspaceAccess.NONE == workspaceAccess) {
                        sqlWorkspaceUsers.remove(sqlWorkspaceUser);
                    } else {
                        sqlWorkspaceUser.setWorkspaceAccess(workspaceAccess);
                    }
                }
            }
            ((SqlWorkspace) workspace).setSqlWorkspaceUser(sqlWorkspaceUsers);
            session.update(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DiffItem> getDiff(Workspace workspace, User user) {
        return new ArrayList<DiffItem>();
    }

    @Override
    public String getCreatorUserId(Workspace workspace, User user) {
        for (WorkspaceUser workspaceUser : findUsersWithAccess(workspace, user)) {
            if (workspaceUser.isCreator()) {
                return workspaceUser.getUserId();
            }
        }
        return null;
    }

    @Override
    public boolean hasWritePermissions(Workspace workspace, User user) {
        return false;
    }

    @Override
    public JSONObject toJson(Workspace workspace, User user, boolean includeVertices) {
        return null;
    }

    @Inject
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
