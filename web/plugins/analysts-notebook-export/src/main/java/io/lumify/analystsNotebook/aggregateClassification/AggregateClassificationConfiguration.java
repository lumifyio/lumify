package io.lumify.analystsNotebook.aggregateClassification;

import io.lumify.core.config.Configurable;
import io.lumify.core.config.PostConfigurationValidator;

public class AggregateClassificationConfiguration {
    public static final String CONFIGURATION_PREFIX = "analystsNotebookExport.aggregateClassificationClient";

    private String serviceUrl;
    private String parameterName;
    private String trustStorePath;
    private String trustStorePassword;
    private boolean disableHostnameVerification;

    @Configurable(name = "serviceUrl", required = false)
    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    @Configurable(name = "parameterName", required = false)
    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    @Configurable(name = "trustStorePath", required = false)
    public void setTrustStorePath(String trustStorePath) {
        this.trustStorePath = trustStorePath;
    }

    @Configurable(name = "trustStorePassword", required = false)
    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    @Configurable(name = "disableHostnameVerification", defaultValue = "false")
    public void setDisableHostnameVerification(String disableHostnameVerification) {
        this.disableHostnameVerification = Boolean.valueOf(disableHostnameVerification);
    }

    @PostConfigurationValidator(description = "both serviceUrl and parameterName must be provided in order to use the aggregate classification client")
    public boolean validateService() {
        return (serviceUrl == null && parameterName == null) || isServiceConfigured();
    }

    @PostConfigurationValidator(description = "both trustStorePath and trustStorePassword must be provided in order to use custom trustStore")
    public boolean validateTrustStore() {
        return (trustStorePath == null && trustStorePassword == null) || isTrustStoreConfigured();
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public String getParameterName() {
        return parameterName;
    }

    public String getTrustStorePath() {
        return trustStorePath;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public boolean isDisableHostnameVerification() {
        return disableHostnameVerification;
    }

    public boolean isServiceConfigured() {
        return serviceUrl != null && parameterName != null;
    }

    public boolean isTrustStoreConfigured() {
        return trustStorePath != null && trustStorePassword != null;
    }
}
