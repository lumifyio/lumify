package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.exception.LumifyException;
import com.altamiracorp.lumify.core.user.User;
import com.altamiracorp.securegraph.Vertex;
import com.google.inject.Inject;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Restrictions;

import java.util.List;
import java.util.Map;

public class SqlUserRepository extends UserRepository{
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
        throw new RuntimeException("not yet implemented");
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
