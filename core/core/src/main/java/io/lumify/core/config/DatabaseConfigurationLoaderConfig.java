package io.lumify.core.config;

public class DatabaseConfigurationLoaderConfig {
    String databaseUrl;
    String databaseUsername;
    String databasePassword;
    int databaseConnectionTimeout;
    String configurationTable;
    String configurationKeyColumn;
    String configurationValueColumn;
    String configurationVersionColumn;
    String configurationKeyPrefix;
    String configurationVersion;

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

    @Configurable(name = "configurationTable")
    public void setConfigurationTable(String configurationTable) {
        this.configurationTable = configurationTable.replaceAll("[^0-9a-zA-Z$_]", "");
    }

    @Configurable(name = "configurationKeyColumn", defaultValue = "k")
    public void setConfigurationKeyColumn(String configurationKeyColumn) {
        this.configurationKeyColumn = configurationKeyColumn.replaceAll("[^0-9a-zA-Z$_]", "");
    }

    @Configurable(name = "configurationValueColumn", defaultValue = "v")
    public void setConfigurationValueColumn(String configurationValueColumn) {
        this.configurationValueColumn = configurationValueColumn.replaceAll("[^0-9a-zA-Z$_]", "");
    }

    @Configurable(name = "configurationVersionColumn", defaultValue = "version")
    public void setConfigurationVersionColumn(String configurationVersionColumn) {
        this.configurationVersionColumn = configurationVersionColumn.replaceAll("[^0-9a-zA-Z$_]", "");
    }

    @Configurable(name = "configurationKeyPrefix")
    public void setConfigurationKeyPrefix(String configurationKeyPrefix) {
        this.configurationKeyPrefix = configurationKeyPrefix;
    }

    @Configurable(name = "configurationVersion")
    public void setConfigurationVersion(String configurationVersion) {
        this.configurationVersion = configurationVersion;
    }
}
