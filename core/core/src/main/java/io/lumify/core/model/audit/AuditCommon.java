package io.lumify.core.model.audit;

import com.altamiracorp.bigtable.model.ColumnFamily;
import com.altamiracorp.bigtable.model.Value;
import io.lumify.web.clientapi.model.UserType;
import io.lumify.core.user.User;
import org.securegraph.Visibility;
import org.json.JSONException;
import org.json.JSONObject;

public class AuditCommon extends ColumnFamily {
    public static final String NAME = "common";
    public static final String ACTOR_TYPE = "actorType";
    public static final String USER_ID = "userId";
    public static final String USER_NAME = "userName";
    public static final String DISPLAY_NAME = "displayName";
    public static final String ACTION = "action";
    public static final String TYPE = "type";
    public static final String COMMENT = "comment";
    public static final String PROCESS = "process";
    public static final String UNIX_BUILD_TIME = "unixBuildTime";
    public static final String VERSION = "version";
    public static final String SCM_BUILD_NUMBER = "scmBuildNumber";
    public static final String PUBLISHED = "published";

    public AuditCommon() {
        super(NAME);
    }

    public String getActorType() {
        return Value.toString(get(ACTOR_TYPE));
    }

    public AuditCommon setActorType(UserType actorType, Visibility visibility) {
        set(ACTOR_TYPE, actorType.toString(), visibility.getVisibilityString());
        return this;
    }

    public String getUserId() {
        return Value.toString(get(USER_ID));
    }

    public AuditCommon setUserId(String userId, Visibility visibility) {
        set(USER_ID, userId, visibility.getVisibilityString());
        return this;
    }

    public String getUserName() {
        return Value.toString(get(USER_NAME));
    }

    public AuditCommon setUserName(String userName, Visibility visibility) {
        set(USER_NAME, userName, visibility.getVisibilityString());
        return this;
    }

    public String getDisplayName() {
        return Value.toString(get(DISPLAY_NAME));
    }

    public AuditCommon setDisplayName(String displayName, Visibility visibility) {
        set(DISPLAY_NAME, displayName, visibility.getVisibilityString());
        return this;
    }

    public String getAction() {
        return Value.toString(get(ACTION));
    }

    public AuditCommon setAction(AuditAction action, Visibility visibility) {
        set(ACTION, action.toString(), visibility.getVisibilityString());
        return this;
    }

    public String getType() {
        return Value.toString(get(TYPE));
    }

    public AuditCommon setType(String type, Visibility visibility) {
        set(TYPE, type, visibility.getVisibilityString());
        return this;
    }

    public String getComment() {
        return Value.toString(get(COMMENT));
    }

    public AuditCommon setComment(String comment, Visibility visibility) {
        set(COMMENT, comment, visibility.getVisibilityString());
        return this;
    }

    public AuditCommon setUser(User user, Visibility visibility) {
        setUserId(user.getUserId(), visibility);
        setUserName(user.getUsername(), visibility);
        setDisplayName(user.getDisplayName(), visibility);
        setActorType(user.getUserType(), visibility);
        return this;
    }

    public String getProcess() {
        return Value.toString(get(PROCESS));
    }

    public AuditCommon setProcess(String process, Visibility visibility) {
        set(PROCESS, process, visibility.getVisibilityString());
        return this;
    }

    public Long getUnixBuildTime() {
        return Value.toLong(get(UNIX_BUILD_TIME));
    }

    public AuditCommon setUnixBuildTime(Long unixBuildTime, Visibility visibility) {
        set(UNIX_BUILD_TIME, unixBuildTime, visibility.getVisibilityString());
        return this;
    }

    public String getVersion() {
        return Value.toString(get(VERSION));
    }

    public AuditCommon setVersion(String version, Visibility visibility) {
        set(VERSION, version, visibility.getVisibilityString());
        return this;
    }

    public String getScmBuildNumber() {
        return Value.toString(get(SCM_BUILD_NUMBER));
    }

    public AuditCommon setScmBuildNumber(String scmBuildNumber, Visibility visibility) {
        set(SCM_BUILD_NUMBER, scmBuildNumber, visibility.getVisibilityString());
        return this;
    }

    public String getPublished() {
        return Value.toString(get(PUBLISHED));
    }

    public AuditCommon setPublished(String published, Visibility visibility) {
        set(PUBLISHED, published, visibility.getVisibilityString());
        return this;
    }

    @Override
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("actorType", getActorType());
            json.put("userId", getUserId());
            json.put("userName", getUserName());
            json.put("displayName", getDisplayName());
            json.put("action", getAction());
            json.put("type", getType());
            json.put("comment", getComment());
            json.put("process", getProcess());
            json.put("unixBuildTime", getUnixBuildTime());
            json.put("version", getVersion());
            json.put("scmBuildNumber", getScmBuildNumber());
            json.put("published", getPublished());
            return json;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
