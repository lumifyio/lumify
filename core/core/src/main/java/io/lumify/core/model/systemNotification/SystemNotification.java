package io.lumify.core.model.systemNotification;

import org.json.JSONObject;

import java.util.Date;

public interface SystemNotification {

    public String getId();

    public void setSeverity(SystemNotificationSeverity severity);

    public SystemNotificationSeverity getSeverity();

    public void setTitle(String title);

    public String getTitle();

    public void setMessage(String message);

    public String getMessage();

    public void setStartDate(Date startDate);

    public Date getStartDate();

    public void setEndDate(Date endDate);

    public Date getEndDate();

    public JSONObject toJSONObject();
}
