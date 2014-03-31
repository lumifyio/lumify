package com.altamiracorp.lumify.sql.model;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.model.user.UserPasswordUtil;
import com.altamiracorp.lumify.core.model.user.UserRepository;
import com.altamiracorp.lumify.core.model.user.UserStatus;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Inject;
import org.apache.commons.lang.StringUtils;
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
    public User findByUserName(String username) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("userName", username)).list();
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
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public User findById(String userId) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public User addUser(String username, String password, String[] userAuthorizations) {
        Session session = sessionFactory.openSession();
        User existingUser = findByUserName(username);
        if (existingUser != null) {
            throw new LumifyException("User, " + username + ", already exists");
        }
        String authorizationsString = StringUtils.join(userAuthorizations, ",");
        byte[] salt = UserPasswordUtil.getSalt();
        byte[] passwordHash = UserPasswordUtil.hashPassword(password, salt);

        Transaction transaction = null;
        SqlUser newUser = null;
        try {
            transaction = session.beginTransaction();
            newUser = new SqlUser ();
            newUser.setUserName(username);
            newUser.setPasswordSalt(salt);
            newUser.setPasswordHash(passwordHash);
            LOGGER.debug("add %s to core user table", username);
            transaction.commit();
        } catch (HibernateException e) {
            transaction.rollback();;
            e.printStackTrace();
        } finally {
            session.close();
        }
        return newUser;
    }

    @Override
    public void setPassword(User user, String password) {

    }

    @Override
    public boolean isPasswordValid(User user, String password) {
        return false;
    }

    @Override
    public User setCurrentWorkspace(String userId, String workspaceId) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public User setStatus(String userId, UserStatus status){
        throw new RuntimeException("not yet implemented");
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
