package io.lumify.sql.model.systemNotification;

import io.lumify.core.model.systemNotification.SystemNotification;
import io.lumify.core.model.systemNotification.SystemNotificationSeverity;
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
    @Column(name = "title")
    public String getTitle() {
        return title;
    }

    @Override
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    @Column(name = "message")
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

    @Override
    public JSONObject toJSONObject() {
        JSONObject json = new JSONObject();
        json.put("id", getId());
        json.put("severity", getSeverity().toString());
        json.put("title", getTitle());
        json.put("message", getMessage());
        json.put("startDate", getStartDate());
        json.put("endDate", getEndDate());
        return json;
    }
}
