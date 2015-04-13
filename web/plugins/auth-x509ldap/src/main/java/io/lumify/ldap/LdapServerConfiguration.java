package io.lumify.ldap;

import io.lumify.core.config.Configurable;

public class LdapServerConfiguration {
    private String primaryLdapServerHostname;
    private int primaryLdapServerPort;
    private String failoverLdapServerHostname;
    private int failoverLdapServerPort;
    private int maxConnections;
    private String bindDn;
    private String bindPassword;
    private String trustStore;
    private String trustStorePassword;
    private String trustStoreType;

    @Configurable(name = "primaryServer")
    public void setPrimaryLdapServerHostname(String primaryLdapServerHostname) {
        this.primaryLdapServerHostname = primaryLdapServerHostname;
    }

    @Configurable(name = "primaryPort", defaultValue = "636")
    public void setPrimaryLdapServerPort(int primaryLdapServerPort) {
        this.primaryLdapServerPort = primaryLdapServerPort;
    }

    @Configurable(name = "failoverServer", required = false)
    public void setFailoverLdapServerHostname(String failoverLdapServerHostname) {
        this.failoverLdapServerHostname = failoverLdapServerHostname;
    }

    @Configurable(name = "failoverPort", defaultValue = "636")
    public void setFailoverLdapServerPort(int failoverLdapServerPort) {
        this.failoverLdapServerPort = failoverLdapServerPort;
    }

    @Configurable(name = "maxConnections", defaultValue = "10")
    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    @Configurable(name = "bindDn")
    public void setBindDn(String bindDn) {
        this.bindDn = bindDn;
    }

    @Configurable(name = "bindPassword")
    public void setBindPassword(String bindPassword) {
        this.bindPassword = bindPassword;
    }

    @Configurable(name = "trustStore")
    public void setTrustStore(String trustStore) {
        this.trustStore = trustStore;
    }

    @Configurable(name = "trustStorePassword")
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @Configurable(name = "trustStoreType", defaultValue = "JKS")
    public void setTrustStoreType(String trustStoreType) {
        this.trustStoreType = trustStoreType;
    }

    public String getPrimaryLdapServerHostname() {
        return primaryLdapServerHostname;
    }

    public int getPrimaryLdapServerPort() {
        return primaryLdapServerPort;
    }

    public String getFailoverLdapServerHostname() {
        return failoverLdapServerHostname;
    }

    public int getFailoverLdapServerPort() {
        return failoverLdapServerPort;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public String getBindDn() {
        return bindDn;
    }

    public String getBindPassword() {
        return bindPassword;
    }

    public String getTrustStore() {
        return trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public String getTrustStoreType() {
        return trustStoreType;
    }
}
