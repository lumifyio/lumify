package io.lumify.bigtable.model.notification.model;

import com.altamiracorp.bigtable.model.*;
import io.lumify.bigtable.model.notification.BigTableUserNotification;
import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.ExpirationAgeUnit;
import org.json.JSONObject;

import java.util.Date;

public class UserNotificationRepository extends Repository<BigTableUserNotification> {
    public UserNotificationRepository(ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public BigTableUserNotification fromRow(Row row) {
        BigTableUserNotification notification = new BigTableUserNotification(new UserNotificationRowKey(row.getRowKey().getRowKey()));
        ColumnFamily cf = row.get(BigTableUserNotification.COLUMN_FAMILY_NAME);
        notification.setTitle(Value.toString(cf.get(BigTableUserNotification.TITLE_COLUMN_NAME)));
        notification.setMessage(Value.toString(cf.get(BigTableUserNotification.MESSAGE_COLUMN_NAME)));
        notification.setUserId(Value.toString(cf.get(BigTableUserNotification.USER_ID_COLUMN_NAME)));
        notification.setSentDate(new Date(Value.toLong(cf.get(BigTableUserNotification.SENT_DATE_COLUMN_NAME))));

        String actionEvent = Value.toString(cf.get(BigTableUserNotification.ACTION_EVENT_COLUMN_NAME));
        if (actionEvent != null) {
            notification.setActionEvent(actionEvent);
        }

        String actionPayload = Value.toString(cf.get(BigTableUserNotification.ACTION_PAYLOAD_COLUMN_NAME));
        if (actionPayload != null) {
            notification.setActionPayload(new JSONObject(actionPayload));
        }

        ExpirationAgeUnit ageUnit = ExpirationAgeUnit.valueOf(Value.toString(cf.get(BigTableUserNotification.EXPIRATION_AGE_UNIT_COLUMN_NAME)));
        Integer ageAmount = Value.toInteger(cf.get(BigTableUserNotification.EXPIRATION_AGE_AMOUNT_COLUMN_NAME));
        if (ageUnit != null && ageAmount != null) {
            notification.setExpirationAge(new ExpirationAge(ageAmount, ageUnit));
        }
        notification.setMarkedRead(Value.toInteger(cf.get(BigTableUserNotification.READ_COLUMN_NAME)) == 1);
        return notification;
    }

    @Override
    public Row toRow(BigTableUserNotification notification) {
        return notification;
    }

    @Override
    public String getTableName() {
        return BigTableUserNotification.TABLE_NAME;
    }
}
