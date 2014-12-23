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
    private MailServerAuthentication mailServerAuthentication;
    private int tokenLifetimeMinutes;
    private String emailFrom;
    private String emailReplyTo;
    private String emailSubject;
    private String newPasswordLabel;
    private String newPasswordConfirmationLabel;

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

    @Configurable(name = "mailServerAuthentication", defaultValue = "NONE")
    public void setMailServerAuthentication(String mailServerAuthentication) {
        this.mailServerAuthentication = MailServerAuthentication.valueOf(mailServerAuthentication);
    }

    @Configurable(name = "tokenLifetimeMinutes", defaultValue = "60")
    public void setTokenLifetimeMinutes(int tokenLifetimeMinutes) {
        this.tokenLifetimeMinutes = tokenLifetimeMinutes;
    }

    @Configurable(name = "emailFrom", required = false)
    public void setEmailFrom(String emailFrom) {
        this.emailFrom = emailFrom;
    }

    @Configurable(name = "emailReplyTo", required = false)
    public void setEmailReplyTo(String emailReplyTo) {
        this.emailReplyTo = emailReplyTo;
    }

    @Configurable(name = "emailSubject", defaultValue = "Forgotten Lumify Password")
    public void setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
    }

    @Configurable(name = "newPasswordLabel", defaultValue = "New Password")
    public void setNewPasswordLabel(String newPasswordLabel) {
        this.newPasswordLabel = newPasswordLabel;
    }

    @Configurable(name = "newPasswordConfirmationLabel", defaultValue = "New Password (again)")
    public void setNewPasswordConfirmationLabel(String newPasswordConfirmationLabel) {
        this.newPasswordConfirmationLabel = newPasswordConfirmationLabel;
    }

    @PostConfigurationValidator(description = "mail server and from address settings are required if the forgot password feature is enabled")
    public boolean validateMailServerSettings() {
        return !enabled || (isNotNullOrBlank(mailServerHostname) &&
                            mailServerPort > 0 &&
                            isNotNullOrBlank(mailServerUsername) &&
                            isNotNullOrBlank(mailServerPassword) &&
                            isNotNullOrBlank(emailFrom) &&
                            isNotNullOrBlank(emailReplyTo)
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

    public MailServerAuthentication getMailServerAuthentication() {
        return mailServerAuthentication;
    }

    public int getTokenLifetimeMinutes() {
        return tokenLifetimeMinutes;
    }

    public String getEmailFrom() {
        return emailFrom;
    }

    public String getEmailReplyTo() {
        return emailReplyTo;
    }

    public String getEmailSubject() {
        return emailSubject;
    }

    public String getNewPasswordLabel() {
        return newPasswordLabel;
    }

    public String getNewPasswordConfirmationLabel() {
        return newPasswordConfirmationLabel;
    }

    private boolean isNotNullOrBlank(String s) {
        return s != null && s.trim().length() > 0;
    }

    public enum MailServerAuthentication {
        NONE,
        TLS,
        SSL
    }
}
