package io.lumify.bigtable.model.systemNotification.model;

import com.altamiracorp.bigtable.model.ModelSession;
import com.altamiracorp.bigtable.model.Repository;
import com.altamiracorp.bigtable.model.Row;
import io.lumify.bigtable.model.systemNotification.BigTableSystemNotification;

public class SystemNotificationRepository extends Repository<BigTableSystemNotification> {
    public SystemNotificationRepository(ModelSession modelSession) {
        super(modelSession);
    }

    @Override
    public BigTableSystemNotification fromRow(Row row) {
        return null;
    }

    @Override
    public Row toRow(BigTableSystemNotification notification) {
        return null;
    }

    @Override
    public String getTableName() {
        return null;
    }
}
