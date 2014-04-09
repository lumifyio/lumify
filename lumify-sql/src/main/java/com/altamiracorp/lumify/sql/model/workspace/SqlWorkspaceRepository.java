package com.altamiracorp.lumify.sql.model.workspace;

import com.altamiracorp.lumify.core.exception.LumifyAccessDeniedException;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.UserRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqlWorkspaceRepository implements WorkspaceRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlWorkspaceRepository.class);
    private SessionFactory sessionFactory;
    private UserRepository userRepository;

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
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public Workspace findById(String workspaceId, User user) {
        Session session = sessionFactory.getCurrentSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).add(Restrictions.eq("id", Integer.parseInt(workspaceId))).list();
        session.close();
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
        SqlWorkspace newWorkspace = null;
        try {
            transaction = session.beginTransaction();
            newWorkspace = new SqlWorkspace();
            newWorkspace.setDisplayTitle(title);
            newWorkspace.setCreator((SqlUser) user);
            session.save(newWorkspace);

            SqlWorkspaceUser sqlWorkspaceUser = new SqlWorkspaceUser();
            sqlWorkspaceUser.setWorkspaceAccess(WorkspaceAccess.WRITE);
            sqlWorkspaceUser.setUser((SqlUser) user);
            sqlWorkspaceUser.setWorkspace(newWorkspace);

            LOGGER.debug("add %s to workspace table", title);
            ((SqlUser) user).getSqlWorkspaceUsers().add(sqlWorkspaceUser);
            session.update(user);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        }
        return newWorkspace;
    }

    @Override
    public Iterable<Workspace> findAll(User user) {
        Session session = sessionFactory.getCurrentSession();
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
        if (!isValidWorkspace(workspace)) {
            throw new LumifyException("Not a valid workspace");
        }

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
            transaction.rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<WorkspaceUser> findUsersWithAccess(Workspace workspace, User user) {
        if (!isValidWorkspace(workspace)) {
            throw new LumifyException("Not a valid workspace");
        }

        if (!doesUserHaveWriteAccess(workspace, user) && !doesUserHaveReadAccess(workspace, user)) {
            throw new LumifyAccessDeniedException("user " + user.getUserId() + " does not have access to workspace " + workspace.getId(), user, workspace.getId());
        }

        Set<SqlWorkspaceUser> sqlWorkspaceUsers = ((SqlWorkspace) workspace).getSqlWorkspaceUser();
        List<WorkspaceUser> withAccess = new ArrayList<WorkspaceUser>();

        for (SqlWorkspaceUser sqlWorkspaceUser : sqlWorkspaceUsers) {
            if (!sqlWorkspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.NONE.toString())) {
                String userId = sqlWorkspaceUser.getUser().getUserId();
                boolean isCreator = workspace.getCreatorUserId().equals(userId);
                withAccess.add(new WorkspaceUser(userId, WorkspaceAccess.valueOf(sqlWorkspaceUser.getWorkspaceAccess()), isCreator));
            }
        }

//        if (readAccess != null && readAccess.size() > 0) {
//            for (SqlUser sqlUser : readAccess) {
//                withAccess.add(new WorkspaceUser(sqlUser.getUserId(), WorkspaceAccess.READ, workspace.getCreatorUserId().equals(sqlUser.getUserId())));
//            }
//        }
//
//        if (writeAccess != null && writeAccess.size() > 0) {
//            for (SqlUser sqlUser : writeAccess) {
//                withAccess.add(new WorkspaceUser(sqlUser.getUserId(), WorkspaceAccess.WRITE, workspace.getCreatorUserId().equals(sqlUser.getUserId())));
//            }
//        }

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

    }

    @Override
    public void updateEntityOnWorkspace(Workspace workspace, Object vertexId, boolean visible, Integer graphPositionX, Integer graphPositionY, User user) {

    }

    @Override
    public void deleteUserFromWorkspace(Workspace workspace, String userId, User user) {
        updateUserOnWorkspace(workspace, userId, WorkspaceAccess.NONE, user);
    }

    @Override
    public boolean doesUserHaveWriteAccess(Workspace workspace, User user) {
        if (!isValidWorkspace(workspace)) {
            throw new LumifyException("Not a valid workspace");
        }
        Set<SqlWorkspaceUser> sqlWorkspaceUsers = ((SqlWorkspace) workspace).getSqlWorkspaceUser();
        for (SqlWorkspaceUser workspaceUser : sqlWorkspaceUsers) {
            if (workspaceUser.getUser().getUserId().equals(user.getUserId()) &&
                    workspaceUser.getWorkspaceAccess().equals(WorkspaceAccess.WRITE.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isValidWorkspace(Workspace workspace) {
        checkNotNull(workspace);
        Session session = sessionFactory.getCurrentSession();
        List workspaces = session.createCriteria(SqlWorkspace.class).add(Restrictions.eq("id", Integer.parseInt(workspace.getId()))).list();
        if (workspaces.size() == 1) {
            return true;
        }
        return false;
    }

    @Override
    public boolean doesUserHaveReadAccess(Workspace workspace, User user) {
        if (!isValidWorkspace(workspace)) {
            throw new LumifyException("Not a valid workspace");
        }
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

        SqlUser userToUpdate = (SqlUser) userRepository.findById(userId);

        Session session = sessionFactory.getCurrentSession();

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            SqlWorkspace sqlWorkspace = (SqlWorkspace) workspace;
//            Set<SqlUser> readAccess = sqlWorkspace.getUserWithReadAccess();
//            Set<SqlUser> writeAccess = sqlWorkspace.getUserWithWriteAccess();
//            if (workspaceAccess == WorkspaceAccess.READ) {
//                if (readAccess == null) {
//                    readAccess = new HashSet<SqlUser>();
//                }
//                readAccess.add(userToUpdate);
//            } else if (workspaceAccess == WorkspaceAccess.WRITE) {
//                if (writeAccess == null) {
//                    writeAccess = new HashSet<SqlUser>();
//                }
//                writeAccess.add(userToUpdate);
//            } else {
//                if (readAccess != null) {
//                    for (SqlUser sqlUser : readAccess) {
//                        if (sqlUser.equals(userId)) {
//                            readAccess.remove(sqlUser);
//                        }
//                    }
//                }
//
//                if (writeAccess != null) {
//                    for (SqlUser sqlUser : writeAccess) {
//                        if (sqlUser.equals(userId)) {
//                            writeAccess.remove(sqlUser);
//                        }
//                    }
//                }
//            }
//            ((SqlWorkspace) workspace).setUserWithReadAccess(readAccess);
//            ((SqlWorkspace) workspace).setUserWithWriteAccess(writeAccess);
            session.update(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<DiffItem> getDiff(Workspace workspace, User user) {
        return new ArrayList<DiffItem>();
    }

    @Inject
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Inject
    public void setUserRepository(UserRepository userRepository) {
        this.userRepository = userRepository;
    }
}
