package io.lumify.sql.model.workspace;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.exception.LumifyAccessDeniedException;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.workspace.*;
import io.lumify.core.model.workspace.diff.DiffItem;
import io.lumify.core.user.ProxyUser;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.sql.model.user.SqlUser;
import io.lumify.sql.model.user.SqlUserRepository;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.securegraph.util.ConvertingIterable;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.securegraph.util.IterableUtils.toList;

@Singleton
public class SqlWorkspaceRepository extends WorkspaceRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlWorkspaceRepository.class);
    private final SqlUserRepository userRepository;
    private final HibernateSessionManager sessionManager;

    @Inject
    public SqlWorkspaceRepository(final SqlUserRepository userRepository, final HibernateSessionManager sessionManager) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
    }

    @Override
    public void delete(Workspace workspace, User user) {
        if (!hasWritePermissions(workspace.getId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            String nullCurrentWorkspacesSql = "update " + SqlUser.class.getSimpleName() + " set current_workspace_id = null where current_workspace_id = :workspaceId";
            session.createQuery(nullCurrentWorkspacesSql).setString("workspaceId", workspace.getId()).executeUpdate();
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
        Session session = sessionManager.getSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).add(Restrictions.eq("workspaceId", Integer.parseInt(workspaceId))).list();
        if (workspaces.size() == 0) {
            return null;
        } else if (workspaces.size() > 1) {
            throw new LumifyException("more than one workspace was returned");
        } else {
            if (!hasReadPermissions(workspaceId, user)) {
                throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspaceId, user, workspaceId);
            }
            return (SqlWorkspace) workspaces.get(0);
        }
    }

    @Override
    public Workspace add(String title, User user) {
        Session session = sessionManager.getSession();

        Transaction transaction = null;
        SqlWorkspace newWorkspace;
        try {
            transaction = session.beginTransaction();
            newWorkspace = new SqlWorkspace();
            newWorkspace.setDisplayTitle(title);
            if (user instanceof ProxyUser) {
                user = userRepository.findById(user.getUserId());
            }
            newWorkspace.setWorkspaceCreator((SqlUser) user);

            SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
            sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);
            sqlWorkspaceUser.setUser((SqlUser) user);
            sqlWorkspaceUser.setWorkspace(newWorkspace);

            LOGGER.debug("add %s to workspace table", title);
            newWorkspace.getSqlWorkspaceUserList().add(sqlWorkspaceUser);
            session.save(newWorkspace);
            session.save(sqlWorkspaceUser);
            session.update(user);
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
        Session session = sessionManager.getSession();
        List workspaces = session.createCriteria(SqlWorkspaceUser.class)
                .add(Restrictions.eq("sqlWorkspaceUser.user.id", Integer.parseInt(user.getUserId())))
                .add(Restrictions.in("workspaceAccess", new String[]{WorkspaceAccess.READ.toString(), WorkspaceAccess.WRITE.toString()}))
                .list();
        return new ConvertingIterable<Object, Workspace>(workspaces) {
            @Override
            protected Workspace convert(Object obj) {
                SqlWorkspaceUser sqlWorkspaceUser = (SqlWorkspaceUser) obj;
                return sqlWorkspaceUser.getWorkspace();
            }
        };
    }

    @Override
    public void setTitle(Workspace workspace, String title, User user) {
        Session session = sessionManager.getSession();
        if (!hasWritePermissions(workspace.getId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

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
    public List<WorkspaceUser> findUsersWithAccess(String workspaceId, User user) {
        List<WorkspaceUser> withAccess = new ArrayList<WorkspaceUser>();
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);

        for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
            if (!sqlWorkspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.NONE.toString())) {
                String userId = sqlWorkspaceUser.getUser().getUserId();
                Workspace workspace = findById(workspaceId, user);
                boolean isCreator = ((SqlWorkspace) workspace).getWorkspaceCreator().getUserId().equals(userId);
                withAccess.add(new WorkspaceUser(userId, WorkspaceAccess.valueOf(sqlWorkspaceUser.getWorkspaceAccess()), isCreator));
            }
        }

        return withAccess;
    }

    @Override
    public List<WorkspaceEntity> findEntities(Workspace workspace, User user) {
        if (!hasReadPermissions(workspace.getId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have read access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionManager.getSession();
        SqlWorkspace sqlWorkspace = (SqlWorkspace) session.get(SqlWorkspace.class, Integer.parseInt(workspace.getId()));
        List<WorkspaceEntity> workspaceEntities = new ArrayList<WorkspaceEntity>();
        List<SqlWorkspaceVertex> sqlWorkspaceVertices = sqlWorkspace.getSqlWorkspaceVertices();
        workspaceEntities = toList(new ConvertingIterable<SqlWorkspaceVertex, WorkspaceEntity>(sqlWorkspaceVertices) {
            @Override
            protected WorkspaceEntity convert(SqlWorkspaceVertex sqlWorkspaceVertex) {
                String vertexId = sqlWorkspaceVertex.getVertexId();

                int graphPositionX = sqlWorkspaceVertex.getGraphPositionX();
                int graphPositionY = sqlWorkspaceVertex.getGraphPositionY();
                boolean visible = sqlWorkspaceVertex.isVisible();

                return new WorkspaceEntity(vertexId, visible, graphPositionX, graphPositionY);
            }
        });
        return workspaceEntities;
    }

    @Override
    public void softDeleteEntityFromWorkspace(Workspace workspace, String vertexId, User user) {
        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            List<SqlWorkspaceVertex> sqlWorkspaceVertices = ((SqlWorkspace) workspace).getSqlWorkspaceVertices();
            for (SqlWorkspaceVertex sqlWorkspaceVertex : sqlWorkspaceVertices) {
                sqlWorkspaceVertex.setVisible(false);
                session.update(sqlWorkspaceVertex);
            }
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void updateEntityOnWorkspace(Workspace workspace, String vertexId, Boolean visible, Integer graphPositionX, Integer graphPositionY, User user) {
        checkNotNull(workspace, "Workspace cannot be null");

        if (!hasWritePermissions(workspace.getId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            List vertices = session.createCriteria(SqlWorkspaceVertex.class)
                    .add(Restrictions.eq("vertexId", vertexId))
                    .add(Restrictions.eq("workspace.workspaceId", Integer.valueOf(workspace.getId())))
                    .list();
            SqlWorkspaceVertex sqlWorkspaceVertex;
            if (vertices.size() > 1) {
                throw new LumifyException("more than one vertex was returned");
            } else if (vertices.size() == 0) {
                sqlWorkspaceVertex = new SqlWorkspaceVertex();
                sqlWorkspaceVertex.setVertexId(vertexId.toString());
                sqlWorkspaceVertex.setWorkspace((SqlWorkspace) workspace);
                ((SqlWorkspace) workspace).getSqlWorkspaceVertices().add(sqlWorkspaceVertex);
                session.update(workspace);
            } else {
                sqlWorkspaceVertex = (SqlWorkspaceVertex) vertices.get(0);
            }
            sqlWorkspaceVertex.setVisible(visible);
            sqlWorkspaceVertex.setGraphPositionX(graphPositionX);
            sqlWorkspaceVertex.setGraphPositionY(graphPositionY);
            session.saveOrUpdate(sqlWorkspaceVertex);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        updateUserOnWorkspace(workspace, userId, WorkspaceAccess.NONE, user);
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
        if (!hasWritePermissions(workspace.getId(), user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have write access to workspace " + workspace.getId(), user, workspace.getId());
        }

        SqlUser userToUpdate = (SqlUser) userRepository.findById(userId);

        Session session = sessionManager.getSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            SqlWorkspace sqlWorkspace = (SqlWorkspace) session.byId(SqlWorkspace.class).load(Integer.parseInt(workspace.getId()));
            List<SqlWorkspaceUser> sqlWorkspaceUsers = sqlWorkspace.getSqlWorkspaceUserList();
            boolean updateUser = false;
            for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
                if (sqlWorkspaceUser.getUser().getUserId().equals(userId)) {
                    updateUser = true;
                    sqlWorkspaceUser.setWorkspaceAccess(workspaceAccess);
                }
            }

            if (!updateUser) {
                SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
                sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);

                sqlWorkspaceUser.setUser(userToUpdate);
                sqlWorkspaceUser.setWorkspace(sqlWorkspace);

                sqlWorkspaceUsers.add(sqlWorkspaceUser);
            }
            session.update(sqlWorkspace);
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
    public boolean hasWritePermissions(String workspaceId, User user) {
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) && workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.WRITE.toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasReadPermissions(String workspaceId, User user) {
        if (hasWritePermissions(workspaceId, user)) {
            return true;
        }
        List<SqlWorkspaceUser> sqlWorkspaceUsers = getSqlWorkspaceUserLists(workspaceId);
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getWorkspace().getId().equals(workspaceId) &&
                    workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.READ.toString())) {
                return true;
            }
        }
        return false;
    }


    protected List<SqlWorkspaceUser> getSqlWorkspaceUserLists(String workspaceId) {
        Session session = sessionManager.getSession();
        List<SqlWorkspaceUser> sqlWorkspaceUsers;
        sqlWorkspaceUsers = session.createCriteria(SqlWorkspaceUser.class).add(Restrictions.eq("sqlWorkspaceUser.workspace.id", Integer.parseInt(workspaceId))).list();
        return sqlWorkspaceUsers;
    }

    protected List<SqlWorkspaceVertex> getSqlWorkspaceVertices(SqlWorkspace sqlWorkspace) {
        Session session = sessionManager.getSession();
        List<SqlWorkspaceVertex> sqlWorkspaceVertices;
        sqlWorkspaceVertices = session.createCriteria(SqlWorkspaceVertex.class).add(Restrictions.eq("workspace.id", Integer.parseInt(sqlWorkspace.getId()))).list();
        return sqlWorkspaceVertices;
    }
}
