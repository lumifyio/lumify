package io.lumify.sql.model.notification;

import io.lumify.core.model.notification.ExpirationAge;
import io.lumify.core.model.notification.ExpirationAgeUnit;
import io.lumify.core.model.notification.UserNotification;
import io.lumify.core.model.notification.UserNotificationRepository;
import org.json.JSONObject;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "user_notification")
public class SqlUserNotification implements UserNotification {
    private String id;
    private String userId;
    private String title;
    private String message;
    private Date sentDate;
    private int expirationAgeAmount;
    private String expirationAgeUnit;
    private boolean markedRead;
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
    public void setUserId(String userId) {
        this.userId = userId;
    }

    @Override
    @Column(name = "user_id")
    public String getUserId() {
        return userId;
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
    public void setSentDate(Date sentDate) {
        if (sentDate == null) {
            sentDate = new Date();
        }
        this.sentDate = sentDate;
    }

    @Override
    @Column(name = "sent_date")
    public Date getSentDate() {
        return sentDate;
    }

    public void setExpirationAgeAmount(int expirationAgeAmount) {
        this.expirationAgeAmount = expirationAgeAmount;
    }

    @Column(name = "expiration_age_amount")
    public int getExpirationAgeAmount() {
        return expirationAgeAmount;
    }

    public void setExpirationAgeUnit(String expirationAgeUnit) {
        this.expirationAgeUnit = expirationAgeUnit;
    }

    @Column(name = "expiration_age_unit")
    public String getExpirationAgeUnit() {
        return expirationAgeUnit;
    }

    @Override
    public void setExpirationAge(ExpirationAge expirationAge) {
        setExpirationAgeAmount(expirationAge.getAmount());
        setExpirationAgeUnit(expirationAge.getExpirationAgeUnit().toString());
    }

    @Override
    @Transient
    public ExpirationAge getExpirationAge() {
        return new ExpirationAge(expirationAgeAmount, ExpirationAgeUnit.valueOf(expirationAgeUnit));
    }

    @Override
    public void setMarkedRead(Boolean markedRead) {
        this.markedRead = markedRead;
    }

    @Override
    @Column(name = "marked_read")
    public Boolean isMarkedRead() {
        return markedRead;
    }

    @Column(name = "action_payload", length = 4000)
    private String getActionPayloadString() {
        return actionPayload;
    }

    public void setActionPayload(String actionPayload) {
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
        this.setActionPayload(jsonData.toString());
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
        return UserNotificationRepository.toJSONObject(this);
    }

    @Transient
    @Override
    public boolean isActive() {
        return UserNotificationRepository.isActive(this);
    }
}
