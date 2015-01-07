package io.lumify.sql.model.notification;

import io.lumify.core.model.notification.Notification;
import io.lumify.core.model.notification.SystemNotification;
import io.lumify.core.model.notification.SystemNotificationRepository;
import io.lumify.core.model.notification.SystemNotificationSeverity;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "system_notification")
public class SqlSystemNotification implements SystemNotification {
    private String id;
    private String severity;
    private String title;
    private String message;
    private Date startDate;
    private Date endDate;
    private String actionEvent;
    private String actionPayload;

    public void setId(String id) {
        this.id = id;
    }

    @Override
    @Id
    @Column(name = "id", unique = true)
    public String getId() {
        return id;
    }

    @Override
    public void setSeverity(SystemNotificationSeverity severity) {
        this.severity = severity.toString();
    }

    @Override
    @Column(name = "severity")
    public SystemNotificationSeverity getSeverity() {
        return SystemNotificationSeverity.valueOf(severity);
    }

    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    @Column(name = "title", length = 1024)
    public String getTitle() {
        return title;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    @Column(name = "message", length = 4000)
    public String getMessage() {
        return message;
    }

    @Override
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    @Column(name = "start_date")
    public Date getStartDate() {
        return startDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    @Column(name = "end_date")
    public Date getEndDate() {
        return endDate;
    }

    @Column(name = "action_payload", length = 4000)
    private String getActionPayloadString() {
        return actionPayload;
    }

    public void setActionPayloadString(String actionPayload) {
        this.actionPayload = actionPayload;
    }

    @Override
    public void setActionEvent(String actionEvent) {
        this.actionEvent = actionEvent;
    }

    @Override
    @Column(name = "action_event")
    public String getActionEvent() {
        return this.actionEvent;
    }

    @Override
    public void setActionPayload(JSONObject jsonData) {
        this.setActionPayloadString(jsonData.toString());
    }

    @Override
    public void setExternalUrl(String externalUrl) {
        this.setActionEvent(Notification.ACTION_EVENT_EXTERNAL_URL);
        JSONObject payload = new JSONObject();
        payload.put("url", externalUrl);
        this.setActionPayload(payload);
    }

    @Transient
    @Override
    public JSONObject getActionPayload() {
        String payload = getActionPayloadString();
        if (payload != null) {
            return new JSONObject(payload);
        }
        return null;
    }

    @Transient
    @Override
    public JSONObject toJSONObject() {
        return SystemNotificationRepository.toJSONObject(this);
    }

    @Transient
    @Override
    public boolean isActive() {
        return SystemNotificationRepository.isActive(this);
    }
}
