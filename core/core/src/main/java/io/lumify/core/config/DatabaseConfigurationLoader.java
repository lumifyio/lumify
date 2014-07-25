package io.lumify.core.config;

import io.lumify.core.exception.LumifyException;

import java.io.*;
import java.sql.*;
import java.util.*;

public class DatabaseConfigurationLoader extends ConfigurationLoader {
    /**
     * !!! DO NOT DEFINE A LOGGER here. This class get loaded very early in the process and we don't want to the logger to be initialized yet **
     */
    public static final String ENV_BOOTSTRAP_LOCATION = "LUMIFY_BOOTSTRAP_LOCATION";
    public static final String DEFAULT_BOOTSTRAP_LOCATION = "/opt/lumify/config";
    public static final String BOOTSTRAP_PREFIX = "databaseConfigurationLoader";

    private DatabaseConfigurationLoaderConfig config = new DatabaseConfigurationLoaderConfig();
    private Map<String, String> configuration;

    public static void main(String[] args) {
        DatabaseConfigurationLoader databaseConfigurationLoader = new DatabaseConfigurationLoader(new HashMap());
        Configuration configuration = databaseConfigurationLoader.createConfiguration();
        System.out.println(configuration);
    }

    /**
     * note that local bootstrap properties will override values in the database
     * */
    public DatabaseConfigurationLoader(Map initParameters) {
        super(initParameters);

        Map<String, String> bootstrapProperties = getBootstrapProperties();
        Configuration bootstrapConfiguration = new Configuration(this, bootstrapProperties);
        bootstrapConfiguration.setConfigurables(config, BOOTSTRAP_PREFIX);

        configuration = getDatabaseProperties();
        configuration.putAll(bootstrapProperties);
    }

    @Override
    public Configuration createConfiguration() {
        return new Configuration(this, configuration);
    }

    /**
     * note that local files in the bootstrap location will override values in the database
     */
    @Override
    public File resolveFileName(String fileName) {
        File bootstrapLocation = getBootstrapLocation();
        if (bootstrapLocation.isDirectory()) {
            File localFile = new File(bootstrapLocation, fileName);
            if (localFile.exists()) {
                return localFile;
            }
        }

        if (configuration.containsKey(fileName)) {
            try {
                File tempFile = File.createTempFile(DatabaseConfigurationLoader.class.getName() + "-", "-" + fileName);
                tempFile.deleteOnExit();

                FileOutputStream fos = new FileOutputStream(tempFile);
                fos.write(configuration.get(fileName).getBytes());
                fos.close();

                return tempFile;
            } catch (IOException ioe) {
                throw new LumifyException("error creating temporary file for file: " + fileName, ioe);
            }
        }

        return null;
    }

    private File getBootstrapLocation() {
        String bootstrapLocationString = System.getenv(ENV_BOOTSTRAP_LOCATION);
        if (bootstrapLocationString == null) {
            bootstrapLocationString = DEFAULT_BOOTSTRAP_LOCATION;
        }
        if (bootstrapLocationString.startsWith("file://")) {
            bootstrapLocationString = bootstrapLocationString.substring("file://".length());
        }
        return new File(bootstrapLocationString);
    }

    private Map<String, String> getBootstrapProperties() {
        Properties properties = new Properties();

        File bootstrapLocation = getBootstrapLocation();
        List<File> bootstrapPropertiesFiles;
        if (bootstrapLocation.isDirectory()) {
            File[] files = bootstrapLocation.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.endsWith(".properties");
                }
            });
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File o1, File o2) {
                    return o1.getName().compareTo(o2.getName());
                }
            });
            bootstrapPropertiesFiles = Arrays.asList(files);
        } else {
            bootstrapPropertiesFiles = new ArrayList<File>();
            bootstrapPropertiesFiles.add(bootstrapLocation);
        }

        for (File file : bootstrapPropertiesFiles) {
            try {
                FileInputStream fis = new FileInputStream(file);
                try {
                    properties.load(fis);
                } finally {
                    fis.close();
                }
            } catch (IOException ioe) {
                throw new LumifyException("error loading bootstrap properties from file: " + file.getName(), ioe);
            }
        }

        Map<String, String> map = new HashMap<String, String>();
        for (Map.Entry entry : properties.entrySet()) {
            map.put((String) entry.getKey(), (String) entry.getValue());
        }
        return map;
    }

    private Map<String, String> getDatabaseProperties() {
        String query = String.format("select %s, %s from %s where %s like ? and %s = ?",
                config.configurationKeyColumn,
                config.configurationValueColumn,
                config.configurationTable,
                config.configurationKeyColumn,
                config.configurationVersionColumn);
        Map<String, String> map = new HashMap<String, String>();

        try {
            Connection conn = DriverManager.getConnection(config.databaseUrl, config.databaseUsername, config.databasePassword);
            try {
                PreparedStatement statement = conn.prepareStatement(query);
                try {
                    statement.setString(1, config.configurationKeyPrefix + ".%");
                    statement.setString(2, config.configurationVersion);

                    ResultSet resultSet = statement.executeQuery();
                    while (resultSet.next()) {
                        String key = resultSet.getString(config.configurationKeyColumn).replaceFirst(config.configurationKeyPrefix + ".", "");
                        String value = resultSet.getString(config.configurationValueColumn);
                        map.put(key, value);
                    }
                } finally {
                    statement.close();
                }
            } finally {
                conn.close();
            }
        } catch (SQLException se) {
            throw new LumifyException("error selecting values from the database", se);
        }
        return map;
    }

}
