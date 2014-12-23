package io.lumify.sql.model.notification;

import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.ExpirationAgeUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public class SqlUserNotificationTest {

    @Test
    public void testSqlUserNotification() {
        SqlUserNotification notification = new SqlUserNotification();
        notification.setId("one");
        notification.setUserId("lumifyUser1");
        notification.setTitle("title");
        notification.setMessage("message");
        notification.setSentDate(new Date());
        notification.setExpirationAge(new ExpirationAge(1, ExpirationAgeUnit.MINUTE));

        assertEquals("one", notification.getId());
        assertEquals("lumifyUser1", notification.getUserId());
        assertEquals("title", notification.getTitle());
        assertEquals("message", notification.getMessage());
        assertTrue(notification.isActive());
    }

    @Test
    public void testFutureActivation() throws InterruptedException {
        SqlUserNotification notification = new SqlUserNotification();
        notification.setExpirationAge(new ExpirationAge(1, ExpirationAgeUnit.MINUTE));
        Calendar cal = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        cal.setTime(new Date());
        cal.add(Calendar.SECOND, 1);
        notification.setSentDate(cal.getTime());

        assertFalse(notification.isActive());
        Thread.sleep(1000);
        assertTrue(notification.isActive());
    }

    @Test
    public void testExpiration() throws InterruptedException {
        SqlUserNotification notification = new SqlUserNotification();
        notification.setExpirationAge(new ExpirationAge(1, ExpirationAgeUnit.SECOND));
        notification.setSentDate(new Date());

        assertTrue(notification.isActive());
        Thread.sleep(1000);
        assertFalse(notification.isActive());
    }

    @Test
    public void testReadMarking() {
        SqlUserNotification notification = new SqlUserNotification();

        assertFalse(notification.isMarkedRead());

        notification.setMarkedRead(true);

        assertTrue(notification.isMarkedRead());
    }
}
