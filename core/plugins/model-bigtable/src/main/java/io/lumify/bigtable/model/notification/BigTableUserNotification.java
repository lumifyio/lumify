package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.Value;
import io.lumify.bigtable.model.notification.model.UserNotificationRowKey;
import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.ExpirationAgeUnit;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.notification.UserNotificationRepository;
import org.json.JSONObject;

import java.util.Date;

public class BigTableUserNotification extends Row<UserNotificationRowKey> implements UserNotification {
    public static final String TABLE_NAME = "lumify_userNotifications";
    public static final String COLUMN_FAMILY_NAME = "";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String MESSAGE_COLUMN_NAME = "message";
    public static final String USER_ID_COLUMN_NAME = "userId";
    public static final String SENT_DATE_COLUMN_NAME = "sentDate";
    public static final String ACTION_EVENT_COLUMN_NAME = "actionEvent";
    public static final String ACTION_PAYLOAD_COLUMN_NAME = "actionPayload";
    public static final String EXPIRATION_AGE_UNIT_COLUMN_NAME = "expirationAgeUnit";
    public static final String EXPIRATION_AGE_AMOUNT_COLUMN_NAME = "expirationAgeAmount";
    public static final String READ_COLUMN_NAME = "read";

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
    public void setUserId(String userId) {
        getColumnFamily().set(USER_ID_COLUMN_NAME, userId);
    }

    @Override
    public String getUserId() {
        return Value.toString(getColumnFamily().get(USER_ID_COLUMN_NAME));
    }


    @Override
    public void setMarkedRead(Boolean markedRead) {
        getColumnFamily().set(READ_COLUMN_NAME, markedRead ? 1 : 0);
    }

    @Override
    public Boolean isMarkedRead() {
        Object val = getColumnFamily().get(READ_COLUMN_NAME);
        return val != null && Value.toInteger(getColumnFamily().get(READ_COLUMN_NAME)) == 1;
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
    public void setActionEvent(String actionEvent) {
        getColumnFamily().set(ACTION_EVENT_COLUMN_NAME, actionEvent);
    }

    @Override
    public String getActionEvent() {
        return Value.toString(getColumnFamily().get(ACTION_EVENT_COLUMN_NAME));
    }

    @Override
    public void setActionPayload(JSONObject jsonData) {
        getColumnFamily().set(ACTION_PAYLOAD_COLUMN_NAME, jsonData.toString());
    }

    @Override
    public JSONObject getActionPayload() {
        String jsonString = Value.toString(getColumnFamily().get(ACTION_PAYLOAD_COLUMN_NAME));
        return jsonString == null ? null : new JSONObject(jsonString);
    }

    @Override
    public void setExpirationAge(ExpirationAge expirationAge) {
        if (expirationAge != null) {
            getColumnFamily().set(EXPIRATION_AGE_AMOUNT_COLUMN_NAME, expirationAge.getAmount());
            getColumnFamily().set(EXPIRATION_AGE_UNIT_COLUMN_NAME, expirationAge.getExpirationAgeUnit().toString());
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
        ExpirationAgeUnit unit = ExpirationAgeUnit.valueOf(Value.toString(getColumnFamily().get(EXPIRATION_AGE_UNIT_COLUMN_NAME)));
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
