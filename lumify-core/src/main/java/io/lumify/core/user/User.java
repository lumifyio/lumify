package io.lumify.core.user;

import com.altamiracorp.bigtable.model.user.ModelUserContext;
import io.lumify.core.model.user.UserType;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.Date;

public interface User extends Serializable {
    public static final long serialVersionUID = 2L;

    public ModelUserContext getModelUserContext();

    public String getUserId();

    public String getUsername();

    public String getDisplayName();

    public String getEmailAddress();

    public Date getCreateDate();

    public Date getCurrentLoginDate();

    public String getCurrentLoginRemoteAddr();

    public Date getPreviousLoginDate();

    public String getPreviousLoginRemoteAddr();

    public int getLoginCount();

    public UserType getUserType();

    public String getUserStatus();

    public String getCurrentWorkspaceId();

    public JSONObject getPreferences();
}
