package io.lumify.core.model.notification;

import java.util.Date;

public interface SystemNotification extends Notification {

    public void setSeverity(SystemNotificationSeverity severity);

    public SystemNotificationSeverity getSeverity();

    public void setStartDate(Date startDate);

    public Date getStartDate();

    public void setEndDate(Date endDate);

    public Date getEndDate();

    public void setExternalUrl(String externalUrl);
}
