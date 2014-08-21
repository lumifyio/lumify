package io.lumify.core.config;

public class DatabaseConfigurationLoaderConfig {
    String databaseDriverClass;
    String databaseUrl;
    String databaseUsername;
    String databasePassword;
    int databaseConnectionTimeout;
    String configurationTable;
    String configurationEnvironmentColumn;
    String configurationVersionColumn;
    String configurationKeyColumn;
    String configurationValueColumn;
    String configurationEnvironment;
    String configurationVersion;
    String configurationKeyPrefix;
    String configurationKeyFileIndicator;

    @Configurable(name = "databaseDriverClass")
    public void setDatabaseDriverClass(String databaseDriverClass) { this.databaseDriverClass = databaseDriverClass; }

    @Configurable(name = "databaseUrl")
    public void setDatabaseUrl(String databaseUrl) {
        this.databaseUrl = databaseUrl;
    }

    @Configurable(name = "databaseUsername")
    public void setDatabaseUsername(String databaseUsername) {
        this.databaseUsername = databaseUsername;
    }

    @Configurable(name = "databasePassword")
    public void setDatabasePassword(String databasePassword) {
        this.databasePassword = databasePassword;
    }

    @Configurable(name = "databaseConnectionTimeout", defaultValue = "10")
    public void setDatabaseConnectionTimeout(String databaseConnectionTimeout) {
        this.databaseConnectionTimeout = Integer.parseInt(databaseConnectionTimeout);
    }

    @Configurable(name = "configurationTable", defaultValue = "configuration")
    public void setConfigurationTable(String configurationTable) {
        this.configurationTable = whitelistSqlIdentifier(configurationTable);
    }

    @Configurable(name = "configurationEnvironmentColumn", defaultValue = "environment")
    public void setConfigurationEnvironmentColumn(String configurationEnvironmentColumn) {
        this.configurationEnvironmentColumn = whitelistSqlIdentifier(configurationEnvironmentColumn);
    }

    @Configurable(name = "configurationVersionColumn", defaultValue = "version")
    public void setConfigurationVersionColumn(String configurationVersionColumn) {
        this.configurationVersionColumn = whitelistSqlIdentifier(configurationVersionColumn);
    }

    @Configurable(name = "configurationKeyColumn", defaultValue = "k")
    public void setConfigurationKeyColumn(String configurationKeyColumn) {
        this.configurationKeyColumn = whitelistSqlIdentifier(configurationKeyColumn);
    }

    @Configurable(name = "configurationValueColumn", defaultValue = "v")
    public void setConfigurationValueColumn(String configurationValueColumn) {
        this.configurationValueColumn = whitelistSqlIdentifier(configurationValueColumn);
    }

    @Configurable(name = "configurationEnvironment")
    public void setConfigurationEnvironment(String configurationEnvironment) {
        this.configurationEnvironment = configurationEnvironment;
    }

    @Configurable(name = "configurationVersion")
    public void setConfigurationVersion(String configurationVersion) {
        this.configurationVersion = configurationVersion;
    }

    @Configurable(name = "configurationKeyPrefix")
    public void setConfigurationKeyPrefix(String configurationKeyPrefix) {
        this.configurationKeyPrefix = configurationKeyPrefix;
    }

    @Configurable(name = "configurationKeyFileIndicator", defaultValue = "FILE")
    public void setConfigurationKeyFileIndicator(String configurationKeyFileIndicator) {
        this.configurationKeyFileIndicator = configurationKeyFileIndicator;
    }

    private String whitelistSqlIdentifier(String string) {
        return string.replaceAll("[^0-9a-zA-Z$_]", "");
    }
}
