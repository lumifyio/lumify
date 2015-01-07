package io.lumify.sql.model.notification;

import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.ExpirationAgeUnit;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.user.InMemoryUser;
import io.lumify.core.user.User;
import io.lumify.sql.model.HibernateSessionManager;
import io.lumify.web.clientapi.model.Privilege;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class SqlUserNotificationRepositoryTest {
    private final String HIBERNATE_IN_MEM_CFG_XML = "hibernateInMem.cfg.xml";
    private HibernateSessionManager sessionManager;
    private SqlUserNotificationRepository sqlUserNotificationRepository;

    @Before
    public void setup() {
        Configuration configuration = new Configuration();
        configuration.configure(HIBERNATE_IN_MEM_CFG_XML);
        ServiceRegistry serviceRegistryBuilder = new StandardServiceRegistryBuilder().applySettings(configuration.getProperties()).build();
        sessionManager = new HibernateSessionManager(configuration.buildSessionFactory(serviceRegistryBuilder));
        sqlUserNotificationRepository = new SqlUserNotificationRepository(sessionManager);
    }

    @After
    public void teardown() {
        sessionManager.clearSession();
    }

    @Test
    public void testCreate() {
        sqlUserNotificationRepository.createNotification("lumifyUser1", "title1", "message1", new ExpirationAge(1, ExpirationAgeUnit.HOUR));
        sqlUserNotificationRepository.createNotification("lumifyUser2", "title2", "message2", new ExpirationAge(1, ExpirationAgeUnit.HOUR));
    }

    // TODO: @Test
    public void testActive() throws InterruptedException {
        User user = new InMemoryUser("lumifyUser3", "Lumify Three", "three@lumify.io", Privilege.stringToPrivileges("READ"), new String[]{}, "workspace3");
        sqlUserNotificationRepository.createNotification(user.getUserId(), "title3a", "message3a", new ExpirationAge(1, ExpirationAgeUnit.SECOND));
        sqlUserNotificationRepository.createNotification(user.getUserId(), "title3b", "message3b", new ExpirationAge(1, ExpirationAgeUnit.MINUTE));
        sqlUserNotificationRepository.createNotification(user.getUserId(), "title3c", "message3c", new ExpirationAge(1, ExpirationAgeUnit.HOUR));
        sqlUserNotificationRepository.createNotification("lumifyUser4", "title4", "message4", new ExpirationAge(1, ExpirationAgeUnit.DAY));
        sqlUserNotificationRepository.createNotification("lumifyUser5", "title5", "message5", new ExpirationAge(1, ExpirationAgeUnit.DAY));

        List<UserNotification> activeNotifications = sqlUserNotificationRepository.getActiveNotifications(user);
        assertEquals(3, activeNotifications.size());
        Thread.sleep(1000);
        activeNotifications = sqlUserNotificationRepository.getActiveNotifications(user);
        assertEquals(2, activeNotifications.size());
    }

    // TODO: @Test
    public void testMarkRead() {
        User user = new InMemoryUser("lumifyUser6", "Lumify Six", "six@lumify.io", Privilege.stringToPrivileges("READ"), new String[]{}, "workspace6");
        UserNotification notification = sqlUserNotificationRepository.createNotification(user.getUserId(), "title6", "message6", new ExpirationAge(1, ExpirationAgeUnit.SECOND));
        sqlUserNotificationRepository.markRead(notification.getId(), user);
    }
}
