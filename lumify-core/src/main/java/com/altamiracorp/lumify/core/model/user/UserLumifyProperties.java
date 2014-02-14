package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.properties.ByteArrayLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;
import com.altamiracorp.securegraph.Vertex;

public final class UserLumifyProperties {
    public static final TextLumifyProperty USERNAME = new TextLumifyProperty("http://lumify.io/lumifyUser/username", TextIndexHint.EXACT_MATCH);
    public static final TextLumifyProperty AUTHORIZATIONS = new TextLumifyProperty("http://lumify.io/lumifyUser/authorizations", TextIndexHint.NONE);
    public static final TextLumifyProperty STATUS = new TextLumifyProperty("http://lumify.io/lumifyUser/status", TextIndexHint.NONE);
    public static final TextLumifyProperty CURRENT_WORKSPACE = new TextLumifyProperty("http://lumify.io/lumifyUser/currentWorkspace", TextIndexHint.EXACT_MATCH);
    public static final ByteArrayLumifyProperty PASSWORD_SALT = new ByteArrayLumifyProperty("http://lumify.io/lumifyUser/passwordSalt");
    public static final ByteArrayLumifyProperty PASSWORD_HASH = new ByteArrayLumifyProperty("http://lumify.io/lumifyUser/passwordHash");

    public static String[] getAuthorizationsArray(Vertex user) {
        String authorizations = AUTHORIZATIONS.getPropertyValue(user);
        if (authorizations == null) {
            return new String[0];
        }
        String[] authorizationsArray = authorizations.split(",");
        if (authorizationsArray.length == 1 && authorizationsArray[0].length() == 0) {
            authorizationsArray = new String[0];
        }
        return authorizationsArray;
    }
}
