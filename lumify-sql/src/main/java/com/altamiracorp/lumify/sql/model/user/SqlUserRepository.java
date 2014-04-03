package com.altamiracorp.lumify.sql.model.user;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.AuthorizationRepository;
import com.altamiracorp.lumify.core.model.user.UserPasswordUtil;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.model.workspace.WorkspaceRepository;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.lumify.sql.model.workspace.SqlWorkspace;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.inject.Inject;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class SqlUserRepository extends UserRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlUserRepository.class);
    private SessionFactory sessionFactory;
    private AuthorizationRepository authorizationRepository;
    private WorkspaceRepository workspaceRepository;

    @Override
    public void init (Map config) {

    };

    @Override
    public User findByDisplayName(String displayName) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("displayName", displayName)).list();
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlUser) users.get(0);
        }
    }

    @Override
    public Iterable<User> findAll() {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).list();
        return new ConvertingIterable<Object, User>(users) {
            @Override
            protected User convert(Object obj) {
                return (SqlUser) obj;
            }
        };
    }

    @Override
    public User findById(String userId) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("id", Integer.parseInt(userId))).list();
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlUser) users.get(0);
        }
    }

    @Override
    public User addUser(String externalId, String displayName, String password, String[] userAuthorizations) {
        Session session = sessionFactory.openSession();
        if (findByDisplayName(displayName) != null) {
            throw new LumifyException("User already exists");
        }

        Transaction transaction = null;
        SqlUser newUser = null;
        try {
            transaction = session.beginTransaction();
            newUser = new SqlUser();
            newUser.setDisplayName(displayName);
            if (password != null && !password.equals("")) {
                byte[] salt = UserPasswordUtil.getSalt();
                byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);
                newUser.setPasswordSalt(salt);
                newUser.setPasswordHash(passwordHash);
            }
            newUser.setExternalId(externalId);
            newUser.setUserStatus(UserStatus.OFFLINE.name());
            LOGGER.debug("add %s to user table", displayName);
            session.save(newUser);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
        return newUser;
    }

    @Override
    public void setPassword(User user, String password) {
        checkNotNull(password);
        Session session = sessionFactory.openSession();
        if (user == null || user.getUserId() == null || findById(user.getUserId()) == null) {
            throw new LumifyException("User is not valid");
        }

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            byte[] salt = UserPasswordUtil.getSalt();
            byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

            ((SqlUser) user).setPasswordSalt(salt);
            ((SqlUser) user).setPasswordHash(passwordHash);
            session.update(user);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        checkNotNull(password);
        if (user == null || (user.getUserId() != null && findById(user.getUserId()) == null)) {
            throw new LumifyException("User is not valid");
        }

        if (((SqlUser) user).getPasswordHash() == null || ((SqlUser) user).getPasswordSalt() == null) {
            return false;
        }
        return UserPasswordUtil.validatePassword(password, ((SqlUser) user).getPasswordSalt(), ((SqlUser) user).getPasswordHash());
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        Session session = sessionFactory.openSession();
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Transaction transaction = null;
        SqlUser sqlUser = null;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser) findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            
            SqlWorkspace sqlWorkspace = (SqlWorkspace)workspaceRepository.findById(workspaceId, sqlUser);
            if (sqlWorkspace == null) {
                throw new LumifyException("workspace does not exist");
            }
            sqlUser.setCurrentWorkspace(sqlWorkspace);
            session.update(sqlUser);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
        return sqlUser;
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        Session session;
        try {
            session = sessionFactory.getCurrentSession();
        } catch (HibernateException e) {
            session = sessionFactory.openSession();
        }
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Transaction transaction = null;
        SqlUser sqlUser = null;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser) findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            sqlUser.setUserStatus(status.name());
            session.update(sqlUser);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
        return sqlUser;
    }

    @Override
    public void addAuthorization(User userUser, String auth) {

    }

    @Override
    public void removeAuthorization(User userUser, String auth) {

    }

    @Override
    public com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        return authorizationRepository.createAuthorizations(new HashSet<String>());
    }

    @Inject
    public void setAuthorizationRepository (AuthorizationRepository authorizationRepository) { this.authorizationRepository = authorizationRepository; }

    @Inject
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
