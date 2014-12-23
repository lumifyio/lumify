package io.lumify.core.model.user;

import io.lumify.core.model.properties.types.*;

public final class UserLumifyProperties {
    public static final StringLumifyProperty USERNAME = new StringLumifyProperty("http://lumify.io/user/username");
    public static final StringLumifyProperty DISPLAY_NAME = new StringLumifyProperty("http://lumify.io/user/displayName");
    public static final StringLumifyProperty EMAIL_ADDRESS = new StringLumifyProperty("http://lumify.io/user/emailAddress");
    public static final DateLumifyProperty CREATE_DATE = new DateLumifyProperty("http://lumify.io/user/createDate");
    public static final DateLumifyProperty CURRENT_LOGIN_DATE = new DateLumifyProperty("http://lumify.io/user/currentLoginDate");
    public static final StringLumifyProperty CURRENT_LOGIN_REMOTE_ADDR = new StringLumifyProperty("http://lumify.io/user/currentLoginRemoteAddr");
    public static final DateLumifyProperty PREVIOUS_LOGIN_DATE = new DateLumifyProperty("http://lumify.io/user/previousLoginDate");
    public static final StringLumifyProperty PREVIOUS_LOGIN_REMOTE_ADDR = new StringLumifyProperty("http://lumify.io/user/previousLoginRemoteAddr");
    public static final IntegerLumifyProperty LOGIN_COUNT = new IntegerLumifyProperty("http://lumify.io/user/loginCount");
    public static final StringLumifyProperty AUTHORIZATIONS = new StringLumifyProperty("http://lumify.io/user/authorizations");
    public static final StringLumifyProperty PRIVILEGES = new StringLumifyProperty("http://lumify.io/user/privileges");
    public static final StringLumifyProperty STATUS = new StringLumifyProperty("http://lumify.io/user/status");
    public static final StringLumifyProperty CURRENT_WORKSPACE = new StringLumifyProperty("http://lumify.io/user/currentWorkspace");
    public static final JsonLumifyProperty UI_PREFERENCES = new JsonLumifyProperty("http://lumify.io/user/uiPreferences");
    public static final ByteArrayLumifyProperty PASSWORD_SALT = new ByteArrayLumifyProperty("http://lumify.io/user/passwordSalt");
    public static final ByteArrayLumifyProperty PASSWORD_HASH = new ByteArrayLumifyProperty("http://lumify.io/user/passwordHash");
    public static final StringLumifyProperty PASSWORD_RESET_TOKEN = new StringLumifyProperty("http://lumify.io/user#passwordResetToken");
    public static final DateLumifyProperty PASSWORD_RESET_TOKEN_EXPIRATION_DATE = new DateLumifyProperty("http://lumify.io/user#passwordResetTokenExpirationDate");
}
