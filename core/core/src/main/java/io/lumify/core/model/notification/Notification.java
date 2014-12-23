package io.lumify.core.model.notification;

import org.json.JSONObject;

/**
 * Created by jharwig on 12/17/14.
 */
public interface Notification {
    String getId();

    void setTitle(String title);

    String getTitle();

    void setMessage(String message);

    String getMessage();

    JSONObject toJSONObject();

    boolean isActive();
}
