package io.lumify.web.auth;

import io.lumify.core.config.Configurable;
import io.lumify.core.config.PostConfigurationValidator;

import java.util.Arrays;
import java.util.List;

public class LdapX509AuthenticationConfiguration {
    private String clientDnHeader;
    private String clientCertHeader;
    private String requiredAttribute;
    private List<String> requiredAttributeValues;
    private List<String> requiredGroups;
    private String usernameAttribute;
    private String displayNameAttribute;

    @Configurable(name = "clientDnHeader", defaultValue = "SSL_CLIENT_S_DN")
    public void setClientDnHeader(String clientDnHeader) {
        this.clientDnHeader = clientDnHeader;
    }

    @Configurable(name = "clientCertHeader", defaultValue = "SSL_CLIENT_CERT")
    public void setClientCertHeader(String clientCertHeader) {
        this.clientCertHeader = clientCertHeader;
    }

    @Configurable(name = "requiredAttribute", required = false)
    public void setRequiredAttribute(String requiredAttribute) {
        this.requiredAttribute = requiredAttribute;
    }

    @Configurable(name = "requiredAttributeValues", required = false)
    public void setRequiredAttributeValues(String requiredAttributeValues) {
        this.requiredAttributeValues = Arrays.asList(requiredAttributeValues.split(","));
    }

    @Configurable(name = "requiredGroups", required = false)
    public void setRequiredGroups(String requiredGroups) {
        this.requiredGroups = Arrays.asList(requiredGroups.split(","));
    }

    @Configurable(name = "usernameAttribute", required = false)
    public void setUsernameAttribute(String usernameAttribute) {
        this.usernameAttribute = usernameAttribute;
    }

    @Configurable(name = "displayNameAttribute", required = false)
    public void setDisplayNameAttribute(String displayNameAttribute) {
        this.displayNameAttribute = displayNameAttribute;
    }

    @PostConfigurationValidator(description = "requiredAttributeValues must be set if requiredAttribute is set")
    public boolean validate() {
        return requiredAttribute == null || requiredAttributeValues != null;
    }

    public String getClientCertHeader() {
        return clientCertHeader;
    }

    public String getClientDnHeader() {
        return clientDnHeader;
    }

    public String getRequiredAttribute() { return requiredAttribute; }

    public List<String> getRequiredAttributeValues() { return requiredAttributeValues; }

    public List<String> getRequiredGroups() { return requiredGroups; }

    public String getUsernameAttribute() {
        return usernameAttribute;
    }

    public String getDisplayNameAttribute() {
        return displayNameAttribute;
    }
}
