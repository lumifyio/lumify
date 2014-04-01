package com.altamiracorp.lumify.sql.model;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.UserPasswordUtil;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.altamiracorp.securegraph.util.ConvertingIterable;
import com.google.inject.Inject;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Map;

public class SqlUserRepository extends UserRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlUserRepository.class);
    private SessionFactory sessionFactory;

    @Override
    public void init(Map map){
    }

    @Override
    public User findByDisplayName(String username) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("name", username)).list();
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlUser)users.get(0);
        }
    }

    @Override
    public Iterable<User> findAll(){
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).list();
        return new ConvertingIterable<Object, User>(users) {
            @Override
            protected User convert(Object obj) {
                return (SqlUser)obj;
            }
        };
    }

    @Override
    public User findById(String userId) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("external_id", userId)).list();
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlUser)users.get(0);
        }
    }

    @Override
    public User addUser(String externalId, String displayName, String password, String[] userAuthorizations) {
        Session session = sessionFactory.openSession();
        User existingUser = findByDisplayName(displayName);
        if (existingUser != null) {
            throw new LumifyException("User, " + displayName + ", already exists");
        }

        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        Transaction transaction = null;
        SqlUser newUser = null;
        try {
            transaction = session.beginTransaction();
            newUser = new SqlUser ();
            newUser.setDisplayName(displayName);
            newUser.setPasswordSalt(salt);
            newUser.setPasswordHash(passwordHash);
            newUser.setExternalId(externalId);
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
        Session session = sessionFactory.openSession();
        if (user == null) {
            throw new LumifyException("User cannot be null");
        }

        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            byte[] salt = UserPasswordUtil.getSalt();
            byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

            ((SqlUser) user).setPasswordSalt(salt);
            ((SqlUser) user).setPasswordHash(passwordHash);
            session.save(user);
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
        if (user == null) {
            throw new LumifyException("User cannot be null");
        }
        return UserPasswordUtil.validatePassword(password, ((SqlUser)user).getPasswordSalt(), ((SqlUser)user).getPasswordHash());
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
            sqlUser = (SqlUser)findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            sqlUser.setCurrentWorkspace(workspaceId);
            session.save(sqlUser);
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
    public User setStatus(String userId, UserStatus status){
        Session session = sessionFactory.openSession();
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Transaction transaction = null;
        SqlUser sqlUser = null;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser)findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            sqlUser.setUserStatus(status);
            session.save(sqlUser);
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
    public com.altamiracorp.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations){
        throw new RuntimeException("not yet implemented");
    }

    @Inject
    public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
}
