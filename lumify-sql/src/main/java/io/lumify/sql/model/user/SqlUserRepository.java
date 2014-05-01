package io.lumify.sql.model.user;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumify.core.config.Configuration;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.model.user.*;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.user.Privilege;
import io.lumify.core.user.User;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.securegraph.util.ConvertingIterable;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@Singleton
public class SqlUserRepository extends UserRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(SqlUserRepository.class);
    private final SessionFactory sessionFactory;
    private final AuthorizationRepository authorizationRepository;
    private final UserListenerUtil userListenerUtil;

    @Inject
    public SqlUserRepository(final Configuration configuration,
                             final AuthorizationRepository authorizationRepository,
                             final SessionFactory sessionFactory,
                             final UserListenerUtil userListenerUtil) {
        super(configuration);
        this.authorizationRepository = authorizationRepository;
        this.sessionFactory = sessionFactory;
        this.userListenerUtil = userListenerUtil;
    }

    @Override
    public User findByUsername(String username) {
        Session session = sessionFactory.openSession();
        List users = session.createCriteria(SqlUser.class).add(Restrictions.eq("username", username)).list();
        session.close();
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
        session.close();
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
        session.close();
        if (users.size() == 0) {
            return null;
        } else if (users.size() > 1) {
            throw new LumifyException("more than one user was returned");
        } else {
            return (SqlUser) users.get(0);
        }
    }

    @Override
    public User addUser(String username, String displayName, String password, String[] userAuthorizations) {
        Session session = sessionFactory.openSession();
        if (findByUsername(username) != null) {
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
            newUser.setUsername(username);
            newUser.setUserStatus(UserStatus.OFFLINE.name());
            newUser.setPrivileges(Privilege.toString(getDefaultPrivileges()));
            LOGGER.debug("add %s to user table", displayName);
            session.save(newUser);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            session.close();
        }

        userListenerUtil.fireNewUserAddedEvent(newUser);

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
            if (transaction != null) {
                transaction.rollback();
            }
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
    public User setCurrentWorkspace(String userId, Workspace workspace) {
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        SqlUser sqlUser = null;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser) findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            sqlUser.setCurrentWorkspace(workspace);
            session.update(sqlUser);
            session.update(workspace);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
        return sqlUser;
    }

    @Override
    public String getCurrentWorkspaceId(String userId) {
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Session session = sessionFactory.openSession();
        try {
            SqlUser sqlUser = (SqlUser) findById(userId);
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            return sqlUser.getCurrentWorkspace().getId();
        } catch (HibernateException e) {
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public User setStatus(String userId, UserStatus status) {
        Session session = sessionFactory.openSession();
        if (userId == null) {
            throw new LumifyException("UserId cannot be null");
        }

        Transaction transaction = null;
        SqlUser sqlUser;
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
            if (transaction != null) {
                transaction.rollback();
            }
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
    public org.securegraph.Authorizations getAuthorizations(User user, String... additionalAuthorizations) {
        return authorizationRepository.createAuthorizations(new HashSet<String>());
    }

    @Override
    public Set<Privilege> getPrivileges(User user) {
        return EnumSet.of(Privilege.READ);
    }

    @Override
    public void delete(User user) {
        Session session = sessionFactory.openSession();

        Transaction transaction = null;
        SqlUser sqlUser;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser) findById(user.getUserId());
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            session.delete(sqlUser);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }

    @Override
    public void setPrivileges(User user, Set<Privilege> privileges) {
        Session session = sessionFactory.openSession();

        Transaction transaction = null;
        SqlUser sqlUser;
        try {
            transaction = session.beginTransaction();
            sqlUser = (SqlUser) findById(user.getUserId());
            if (sqlUser == null) {
                throw new LumifyException("User does not exist");
            }
            sqlUser.setPrivileges(Privilege.toString(privileges));
            session.update(sqlUser);
            transaction.commit();
        } catch (HibernateException e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        } finally {
            session.close();
        }
    }
}
