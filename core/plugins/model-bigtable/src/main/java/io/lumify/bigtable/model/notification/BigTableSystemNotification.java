package io.lumify.bigtable.model.notification;

import com.altamiracorp.bigtable.model.Column;
import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Row;
import com.altamiracorp.bigtable.model.Value;
import io.lumify.bigtable.model.notification.model.SystemNotificationRowKey;
import io.lumify.core.model.notification.SystemNotification;
import io.lumify.core.model.notification.SystemNotificationRepository;
import io.lumify.core.model.notification.SystemNotificationSeverity;
import org.json.JSONObject;

import java.util.Date;

public class BigTableSystemNotification extends Row<SystemNotificationRowKey> implements SystemNotification {
    public static final String TABLE_NAME = "lumify_systemNotifications";
    public static final String COLUMN_FAMILY_NAME = "";
    public static final String SEVERITY_COLUMN_NAME = "severity";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String MESSAGE_COLUMN_NAME = "message";
    public static final String START_DATE_COLUMN_NAME = "startDate";
    public static final String END_DATE_COLUMN_NAME = "endDate";

    public BigTableSystemNotification(SystemNotificationRowKey rowKey) {
        super(TABLE_NAME, rowKey);
    }

    public BigTableSystemNotification() {
        super(TABLE_NAME);
    }

    @Override
    public String getId() {
        return getRowKey().getRowKey();
    }

    public void setSeverity(SystemNotificationSeverity severity) {
        getColumnFamily().set(SEVERITY_COLUMN_NAME, severity.toString());
    }

    @Override
    public SystemNotificationSeverity getSeverity() {
        return SystemNotificationSeverity.valueOf(Value.toString(getColumnFamily().get(SEVERITY_COLUMN_NAME)));
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

    public void setStartDate(Date startDate) {
        if (startDate == null) {
            startDate = new Date();
        }
        getColumnFamily().set(START_DATE_COLUMN_NAME, startDate.getTime());
    }

    @Override
    public Date getStartDate() {
        return new Date(Value.toLong(getColumnFamily().get(START_DATE_COLUMN_NAME)));
    }

    public void setEndDate(Date endDate) {
        if (endDate != null) {
            getColumnFamily().set(END_DATE_COLUMN_NAME, endDate.getTime());
        } else {
            boolean existingEndDate = false;
            for (Column column : getColumnFamily().getColumns()) {
                if (column.getName().equals(END_DATE_COLUMN_NAME)) {
                    existingEndDate = true;
                    break;
                }
            }
            if (existingEndDate) {
                throw new IllegalArgumentException("unable to update to a null end date");
            }
            // it's ok for it to never be set which will behave like a null
        }
    }

    @Override
    public Date getEndDate() {
        Long endDate = Value.toLong(getColumnFamily().get(END_DATE_COLUMN_NAME));
        if (endDate != null) {
            return new Date(endDate);
        }
        return null;
    }

    @Override
    public JSONObject toJSONObject() {
        return SystemNotificationRepository.toJSONObject(this);
    }

    @Override
    public boolean isActive() {
        return SystemNotificationRepository.isActive(this);
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
