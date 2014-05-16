package io.lumify.core.model.user;

import io.lumify.core.model.properties.types.ByteArrayLumifyProperty;
import io.lumify.core.model.properties.types.StringLumifyProperty;
import org.securegraph.TextIndexHint;

public final class UserLumifyProperties {
    public static final StringLumifyProperty USERNAME = new StringLumifyProperty("http://lumify.io/user/username");
    public static final StringLumifyProperty DISPLAY_NAME = new StringLumifyProperty("http://lumify.io/user/displayName");
    public static final StringLumifyProperty AUTHORIZATIONS = new StringLumifyProperty("http://lumify.io/user/authorizations");
    public static final StringLumifyProperty PRIVILEGES = new StringLumifyProperty("http://lumify.io/user/privileges");
    public static final StringLumifyProperty STATUS = new StringLumifyProperty("http://lumify.io/user/status");
    public static final StringLumifyProperty CURRENT_WORKSPACE = new StringLumifyProperty("http://lumify.io/user/currentWorkspace");
    public static final ByteArrayLumifyProperty PASSWORD_SALT = new ByteArrayLumifyProperty("http://lumify.io/user/passwordSalt");
    public static final ByteArrayLumifyProperty PASSWORD_HASH = new ByteArrayLumifyProperty("http://lumify.io/user/passwordHash");
}
