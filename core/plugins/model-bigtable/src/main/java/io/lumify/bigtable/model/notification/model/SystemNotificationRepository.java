package io.lumify.bigtable.model.notification.model;

import com.altamiracorp.bigtable.model.*;
import io.lumify.bigtable.model.notification.BigTableSystemNotification;
import io.lumify.core.model.notification.SystemNotificationSeverity;
import org.json.JSONObject;

import java.util.Date;

public class SystemNotificationRepository extends Repository<BigTableSystemNotification> {
    public SystemNotificationRepository(ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public BigTableSystemNotification fromRow(Row row) {
        BigTableSystemNotification notification = new BigTableSystemNotification(new SystemNotificationRowKey(row.getRowKey().getRowKey()));
        ColumnFamily cf = row.get(BigTableSystemNotification.COLUMN_FAMILY_NAME);
        notification.setSeverity(SystemNotificationSeverity.valueOf(Value.toString(cf.get(BigTableSystemNotification.SEVERITY_COLUMN_NAME))));
        notification.setTitle(Value.toString(cf.get(BigTableSystemNotification.TITLE_COLUMN_NAME)));
        notification.setMessage(Value.toString(cf.get(BigTableSystemNotification.MESSAGE_COLUMN_NAME)));
        notification.setActionEvent(Value.toString(cf.get(BigTableSystemNotification.ACTION_EVENT_COLUMN_NAME)));

        String payloadData = Value.toString(cf.get(BigTableSystemNotification.ACTION_PAYLOAD_COLUMN_NAME));
        if (payloadData != null) {
            notification.setActionPayload(new JSONObject(payloadData));
        }

        notification.setStartDate(new Date(Value.toLong(cf.get(BigTableSystemNotification.START_DATE_COLUMN_NAME))));
        Long endDate = Value.toLong(cf.get(BigTableSystemNotification.END_DATE_COLUMN_NAME));
        if (endDate != null) {
            notification.setEndDate(new Date(endDate));
        }
        return notification;
    }

    @Override
    public Row toRow(BigTableSystemNotification notification) {
        return notification;
    }

    @Override
    public String getTableName() {
        return BigTableSystemNotification.TABLE_NAME;
    }
}
