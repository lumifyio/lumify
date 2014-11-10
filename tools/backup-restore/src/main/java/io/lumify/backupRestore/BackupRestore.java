package io.lumify.backupRestore;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupRestore {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestore.class);

    private static final SimpleDateFormat backupDirectoryFormat = new SimpleDateFormat("yyyyMMdd'T'HHmm");
    private static final String CMD_OPT_TABLE_NAME_PREFIX = "tableprefix";
    private static final String DEFAULT_TABLE_NAME_PREFIX = "lumify";
    private static final String CMD_OPT_ACCUMULO_INSTANCE_NAME = "accumuloInstanceName";
    private static final String CMD_OPT_ACCUMULO_USER_NAME = "accumuloUserName";
    private static final String CMD_OPT_ACCUMULO_PASSWORD = "accumuloPassword";
    private static final String CMD_OPT_ZOOKEEPER_SERVERS = "zookeeperServers";
    private static final String CMD_OPT_HDFS_BACKUP_DIRECTORY = "hdfsBackupDirectory";
    private static final String CMD_OPT_HDFS_RESTORE_DIRECTORY = "hdfsRestoreDirectory";
    private static final String CMD_OPT_HDFS_LOCATION = "hdfsLocation";
    private String accumuloInstanceName;
    private String accumuloUserName;
    private String accumuloPassword;
    private String zookeeperServers;
    private String hdfsLocation;

    public static void main(String[] args) {
        new BackupRestore().run(args);
    }

    private void run(String[] args) {
        CommandLine cmd = parseOptions(args);
        if (cmd == null) {
            System.exit(-1);
            return;
        }

        String tableNamePrefix = cmd.getOptionValue(CMD_OPT_TABLE_NAME_PREFIX);
        if (tableNamePrefix == null) {
            tableNamePrefix = DEFAULT_TABLE_NAME_PREFIX;
        }

        accumuloInstanceName = cmd.getOptionValue(CMD_OPT_ACCUMULO_INSTANCE_NAME);
        accumuloUserName = cmd.getOptionValue(CMD_OPT_ACCUMULO_USER_NAME);
        accumuloPassword = cmd.getOptionValue(CMD_OPT_ACCUMULO_PASSWORD);
        zookeeperServers = cmd.getOptionValue(CMD_OPT_ZOOKEEPER_SERVERS);
        hdfsLocation = cmd.getOptionValue(CMD_OPT_HDFS_LOCATION);
        if (hdfsLocation == null) {
            System.out.print(CMD_OPT_HDFS_LOCATION + " is required");
            System.exit(-1);
            return;
        }

        String[] restOfArgs = cmd.getArgs();
        if (restOfArgs.length != 1) {
            System.out.print("Either backup or restore must be specified.");
            System.exit(-1);
            return;
        }

        Action action;
        if (restOfArgs[0].equals("backup")) {
            action = Action.BACKUP;
        } else if (restOfArgs[0].equals("restore")) {
            action = Action.RESTORE;
        } else {
            System.out.print("Either backup or restore must be specified.");
            System.exit(-1);
            return;
        }

        switch (action) {
            case BACKUP:
                try {
                    String hdfsBackupDirectory = cmd.getOptionValue(CMD_OPT_HDFS_BACKUP_DIRECTORY);
                    if (hdfsBackupDirectory == null) {
                        hdfsBackupDirectory = "/backup/" + backupDirectoryFormat.format(new Date());
                    }

                    BackupOptions backupOptions = new BackupOptions()
                            .setHdfsBackupDirectory(hdfsBackupDirectory)
                            .setTableNamePrefix(tableNamePrefix);
                    setCommonOptions(backupOptions);
                    new Backup().run(backupOptions);
                } catch (Exception ex) {
                    LOGGER.error("Failed to backup", ex);
                    System.exit(-1);
                    return;
                }
                break;
            case RESTORE:
                try {
                    String hdfsRestoreDirectory = cmd.getOptionValue(CMD_OPT_HDFS_RESTORE_DIRECTORY);

                    if (hdfsRestoreDirectory == null) {
                        System.out.println(CMD_OPT_HDFS_RESTORE_DIRECTORY + " is required for restore");
                        System.exit(-1);
                        return;
                    }
                    RestoreOptions restoreOptions = new RestoreOptions()
                            .setHdfsRestoreDirectory(hdfsRestoreDirectory);
                    setCommonOptions(restoreOptions);
                    new Restore().run(restoreOptions);
                } catch (Exception ex) {
                    LOGGER.error("Failed to restore", ex);
                    System.exit(-1);
                    return;
                }
                break;
            default:
                throw new RuntimeException("Invalid action: " + action);
        }
    }

    private void setCommonOptions(BackupRestoreOptionsBase options) {
        options
                .setAccumuloInstanceName(accumuloInstanceName)
                .setAccumuloUserName(accumuloUserName)
                .setAccumuloPassword(accumuloPassword)
                .setZookeeperServers(zookeeperServers)
                .setHdfsLocation(hdfsLocation);
    }

    private CommandLine parseOptions(String[] args) {
        Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Print help")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_TABLE_NAME_PREFIX)
                        .hasArg()
                        .withDescription("Table name prefix of tables to backup")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ACCUMULO_INSTANCE_NAME)
                        .hasArg()
                        .withDescription("Accumulo instance name")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ACCUMULO_USER_NAME)
                        .hasArg()
                        .withDescription("Accumulo user name")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ACCUMULO_PASSWORD)
                        .hasArg()
                        .withDescription("Accumulo password")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ZOOKEEPER_SERVERS)
                        .hasArg()
                        .withDescription("Zookeeper servers")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_BACKUP_DIRECTORY)
                        .hasArg()
                        .withDescription("Directory to store backups in HDFS")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_RESTORE_DIRECTORY)
                        .hasArg()
                        .withDescription("Directory to restore backups from in HDFS")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_LOCATION)
                        .hasArg()
                        .withDescription("Location of HDFS")
                        .create()
        );

        CommandLine cmd;
        try {
            CommandLineParser parser = new GnuParser();
            cmd = parser.parse(options, args);
            if (cmd.hasOption("help")) {
                printHelp(options);
                return null;
            }
            return cmd;
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            printHelp(options);
        }
        return null;
    }

    protected void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("run", options, true);
    }
}
