package io.lumify.web.clientapi.model;

import org.json.JSONArray;
import org.json.JSONObject;

public class UserMeResponse {
    private final JSONObject responseJson;

    public UserMeResponse(JSONObject responseJson) {
        this.responseJson = responseJson;
    }

    public String getId() {
        return this.responseJson.getString("id");
    }

    public String[] getAuthorizations() {
        JSONArray authorizations = this.responseJson.getJSONArray("authorizations");
        String[] results = new String[authorizations.length()];
        for (int i = 0; i < authorizations.length(); i++) {
            results[i] = authorizations.getString(i);
        }
        return results;
    }

    public UserStatus getStatus() {
        return UserStatus.valueOf(this.responseJson.getString("status"));
    }

    public String getCsrfToken() {
        return this.responseJson.getString("csrfToken");
    }

    public String getUserName() {
        return this.responseJson.getString("userName");
    }

    public Privilege[] getPrivileges() {
        JSONArray privileges = this.responseJson.getJSONArray("privileges");
        Privilege[] results = new Privilege[privileges.length()];
        for (int i = 0; i < privileges.length(); i++) {
            results[i] = Privilege.valueOf(privileges.getString(i));
        }
        return results;
    }

    public String getDisplayName() {
        return this.responseJson.getString("displayName");
    }

    public String getCurrentWorkspaceId() {
        return this.responseJson.optString("currentWorkspaceId", null);
    }

    @Override
    public String toString() {
        return "UserMeResponse {" +
                "responseJson=" + responseJson +
                '}';
    }
}
