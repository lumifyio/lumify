package com.altamiracorp.lumify.core.model.user;

import com.altamiracorp.lumify.core.model.properties.types.ByteArrayLumifyProperty;
import com.altamiracorp.lumify.core.model.properties.types.TextLumifyProperty;
import com.altamiracorp.securegraph.TextIndexHint;

public final class UserLumifyProperties {
    public static final TextLumifyProperty USERNAME = new TextLumifyProperty("http://lumify.io/user/username", TextIndexHint.EXACT_MATCH);
    public static final TextLumifyProperty AUTHORIZATIONS = new TextLumifyProperty("http://lumify.io/user/authorizations", TextIndexHint.NONE);
    public static final TextLumifyProperty STATUS = new TextLumifyProperty("http://lumify.io/user/status", TextIndexHint.NONE);
    public static final TextLumifyProperty CURRENT_WORKSPACE = new TextLumifyProperty("http://lumify.io/user/currentWorkspace", TextIndexHint.EXACT_MATCH);
    public static final ByteArrayLumifyProperty PASSWORD_SALT = new ByteArrayLumifyProperty("http://lumify.io/user/passwordSalt");
    public static final ByteArrayLumifyProperty PASSWORD_HASH = new ByteArrayLumifyProperty("http://lumify.io/user/passwordHash");
}
