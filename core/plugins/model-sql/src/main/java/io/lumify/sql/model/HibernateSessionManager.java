package io.lumify.sql.model;


import org.hibernate.Session;
import org.hibernate.SessionFactory;

public class HibernateSessionManager {
    private static SessionFactory sessionFactory;
    private static ThreadLocal<Session> threadLocalSession = new InheritableThreadLocal<Session>() {
        @Override
        protected Session initialValue() {
            return sessionFactory.openSession();
        }
    };

    // this is to prevent opening a session just to clear it
    private static ThreadLocal<Boolean> threadLocalSessionIsOpen = new InheritableThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static void initialize(SessionFactory factory) {
        sessionFactory = factory;
        threadLocalSession.remove();
    }

    public static Session getSession() {
        threadLocalSessionIsOpen.set(Boolean.TRUE);
        return threadLocalSession.get();
    }

    public static void clearSession() {
        if (threadLocalSessionIsOpen.get()) {
            threadLocalSession.get().close();
        }

        threadLocalSession.remove();
        threadLocalSessionIsOpen.remove();
    }

}
