package io.lumify.web.auth.usernamepassword;

import io.lumify.core.config.Configurable;
import io.lumify.core.config.PostConfigurationValidator;

public class ForgotPasswordConfiguration {
    public static final String CONFIGURATION_PREFIX = "forgotPassword";

    private boolean enabled;
    private String mailServerHostname;
    private int mailServerPort;
    private String mailServerUsername;
    private String mailServerPassword;
    private int tokenLifetimeMinutes;

    @Configurable(name = "enabled", defaultValue = "false")
    public void setEnabled(String enabled) {
        this.enabled = Boolean.valueOf(enabled);
    }

    @Configurable(name = "mailServerHostname", required = false)
    public void setMailServerHostname(String mailServerHostname) {
        this.mailServerHostname = mailServerHostname;
    }

    @Configurable(name = "mailServerPort", required = false)
    public void setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
    }

    @Configurable(name = "mailServerUsername", required = false)
    public void setMailServerUsername(String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
    }

    @Configurable(name = "mailServerPassword", required = false)
    public void setMailServerPassword(String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
    }

    @Configurable(name = "tokenLifetimeMinutes", defaultValue = "60")
    public void setTokenLifetimeMinutes(int tokenLifetimeMinutes) {
        this.tokenLifetimeMinutes = tokenLifetimeMinutes;
    }

    @PostConfigurationValidator(description = "mail server settings are required if the forgot password feature is enabled")
    public boolean validateMailServerSettings() {
        return !enabled || (isNotNullOrBlank(mailServerHostname) &&
                            mailServerPort > 0 &&
                            isNotNullOrBlank(mailServerUsername) &&
                            isNotNullOrBlank(mailServerPassword)
        );
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getMailServerHostname() {
        return mailServerHostname;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public String getMailServerUsername() {
        return mailServerUsername;
    }

    public int getTokenLifetimeMinutes() {
        return tokenLifetimeMinutes;
    }

    private boolean isNotNullOrBlank(String s) {
        return s != null && s.trim().length() > 0;
    }
}
