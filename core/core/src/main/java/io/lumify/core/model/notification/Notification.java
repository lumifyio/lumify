package io.lumify.core.model.notification;

import org.json.JSONObject;

public interface Notification {

    public static final String ACTION_EVENT_EXTERNAL_URL = "EXTERNAL_URL";

    String getId();

    void setTitle(String title);

    String getTitle();

    void setMessage(String message);

    String getMessage();

    void setActionEvent(String actionEvent);

    String getActionEvent();

    void setActionPayload(JSONObject jsonData);

    JSONObject getActionPayload();

    JSONObject toJSONObject();

    boolean isActive();
}
