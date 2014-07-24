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

    public static void initialize(SessionFactory factory) {
        sessionFactory = factory;
        threadLocalSession.remove();
    }

    public static Session getSession() {
        return threadLocalSession.get();
    }

    public static void clearSession() {
        threadLocalSession.get().close();
        threadLocalSession.remove();
    }

}
