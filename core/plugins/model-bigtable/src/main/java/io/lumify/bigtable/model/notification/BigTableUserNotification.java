package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.Value;
import io.lumify.bigtable.model.notification.model.UserNotificationRowKey;
import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.notification.UserNotificationRepository;
import org.json.JSONObject;

import java.util.Date;

public class BigTableUserNotification extends Row<UserNotificationRowKey> implements UserNotification {
    public static final String TABLE_NAME = "lumify_userNotifications";
    public static final String COLUMN_FAMILY_NAME = "";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String MESSAGE_COLUMN_NAME = "message";
    public static final String USER_COLUMN_NAME = "user";
    public static final String SENT_DATE_COLUMN_NAME = "startDate";
    public static final String EXPIRATION_AGE_UNIT_COLUMN_NAME = "expirationAgeUnit";
    public static final String EXPIRATION_AGE_AMOUNT_COLUMN_NAME = "expirationAgeAmount";

    public BigTableUserNotification(UserNotificationRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public BigTableUserNotification() {
        super(TABLE_NAME);
    }

    @Override
    public String getId() {
        return getRowKey().getRowKey();
    }

    public void setTitle(String title) {
        getColumnFamily().set(TITLE_COLUMN_NAME, title);
    }

    @Override
    public String getTitle() {
        return Value.toString(getColumnFamily().get(TITLE_COLUMN_NAME));
    }

    public void setMessage(String message) {
        getColumnFamily().set(MESSAGE_COLUMN_NAME, message);
    }

    @Override
    public String getMessage() {
        return Value.toString(getColumnFamily().get(MESSAGE_COLUMN_NAME));
    }

    @Override
    public void setUser(String userId) {
        getColumnFamily().set(USER_COLUMN_NAME, userId);
    }

    @Override
    public String getUser() {
        return Value.toString(getColumnFamily().get(USER_COLUMN_NAME));
    }

    @Override
    public void setSentDate(Date sentDate) {
        if (sentDate == null) {
            sentDate = new Date();
        }
        getColumnFamily().set(SENT_DATE_COLUMN_NAME, sentDate.getTime());
    }

    @Override
    public Date getSentDate() {
        return new Date(Value.toLong(getColumnFamily().get(SENT_DATE_COLUMN_NAME)));
    }

    @Override
    public void setExpirationAge(ExpirationAge expirationAge) {
        if (expirationAge != null) {
            getColumnFamily().set(EXPIRATION_AGE_AMOUNT_COLUMN_NAME, expirationAge.getAmount());
            getColumnFamily().set(EXPIRATION_AGE_UNIT_COLUMN_NAME, expirationAge.getCalendarUnit());
        } else {
            boolean existingExpiration = false;
            for (Column column : getColumnFamily().getColumns()) {
                if (column.getName().equals(EXPIRATION_AGE_AMOUNT_COLUMN_NAME) ||
                    column.getName().equals(EXPIRATION_AGE_UNIT_COLUMN_NAME)) {
                    existingExpiration = true;
                    break;
                }
            }
            if (existingExpiration) {
                throw new IllegalArgumentException("unable to update to a null expiration");
            }
        }
    }

    @Override
    public ExpirationAge getExpirationAge() {
        Integer amount = Value.toInteger(getColumnFamily().get(EXPIRATION_AGE_AMOUNT_COLUMN_NAME));
        Integer unit = Value.toInteger(getColumnFamily().get(EXPIRATION_AGE_UNIT_COLUMN_NAME));
        if (unit != null && amount != null) {
            return new ExpirationAge(amount, unit);
        }
        return null;
    }

    @Override
    public JSONObject toJSONObject() {
        return UserNotificationRepository.toJSONObject(this);
    }

    @Override
    public boolean isActive() {
        return UserNotificationRepository.isActive(this);
    }
    
    private ColumnFamily getColumnFamily() {
        ColumnFamily cf = get(COLUMN_FAMILY_NAME);
        if (cf == null) {
            cf = new ColumnFamily(COLUMN_FAMILY_NAME);
            addColumnFamily(cf);
        }
        return cf;
    }
}
