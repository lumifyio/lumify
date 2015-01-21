package io.lumify.backupRestore;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class BackupRestore {
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupRestore.class);

    private static final SimpleDateFormat BACKUP_DIRECTORY_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmm");
    private static final String CMD_OPT_TABLE_NAME_PREFIX = "tableNamePrefix";
    private static final String DEFAULT_TABLE_NAME_PREFIX = "lumify_";
    private static final String CMD_OPT_ACCUMULO_INSTANCE_NAME = "accumuloInstanceName";
    private static final String CMD_OPT_ACCUMULO_USERNAME = "accumuloUsername";
    private static final String CMD_OPT_ACCUMULO_PASSWORD = "accumuloPassword";
    private static final String CMD_OPT_ZOOKEEPER_SERVERS = "zookeeperServers";
    private static final String CMD_OPT_HDFS_BACKUP_DIRECTORY = "hdfsBackupDirectory";
    private static final String DEFAULT_HDFS_BACKUP_DIRECTORY = "/backup/" + BACKUP_DIRECTORY_DATE_FORMAT.format(new Date());
    private static final String CMD_OPT_HDFS_RESTORE_DIRECTORY = "hdfsRestoreDirectory";
    private static final String CMD_OPT_HDFS_RESTORE_TEMP_DIRECTORY = "hdfsRestoreTempDirectory";
    private static final String CMD_OPT_HADOOP_FS_DEFAULT_FS = "hadoopFsDefaultFS";
    private static final String CMD_OPT_HADOOP_DFS_CLIENT_USE_DATANODE_HOSTNAME = "hadoopDfsClientUseDatanodeHostname";
    private static final String CMD_OPT_HADOOP_USERNAME = "hadoopUsername";

    private String accumuloInstanceName;
    private String accumuloUsername;
    private String accumuloPassword;
    private String zookeeperServers;
    private String hadoopFsDefaultFs;
    private boolean hadoopDfsClientUseDatanodeHostname;
    private String hadoopUsername;

    public static void main(String[] args) {
        new BackupRestore().run(args);
    }

    private void run(String[] args) {
        CommandLine cmd = parseOptions(args);
        if (cmd == null) {
            System.exit(-1);
            return;
        }

        String tableNamePrefix = cmd.getOptionValue(CMD_OPT_TABLE_NAME_PREFIX, DEFAULT_TABLE_NAME_PREFIX);
        accumuloInstanceName = cmd.getOptionValue(CMD_OPT_ACCUMULO_INSTANCE_NAME);
        accumuloUsername = cmd.getOptionValue(CMD_OPT_ACCUMULO_USERNAME);
        accumuloPassword = cmd.getOptionValue(CMD_OPT_ACCUMULO_PASSWORD);
        zookeeperServers = cmd.getOptionValue(CMD_OPT_ZOOKEEPER_SERVERS);
        hadoopFsDefaultFs = cmd.getOptionValue(CMD_OPT_HADOOP_FS_DEFAULT_FS);
        if (hadoopFsDefaultFs == null) {
            System.out.println(CMD_OPT_HADOOP_FS_DEFAULT_FS + " is required");
            System.exit(-1);
            return;
        }
        hadoopDfsClientUseDatanodeHostname = cmd.hasOption(CMD_OPT_HADOOP_DFS_CLIENT_USE_DATANODE_HOSTNAME);
        hadoopUsername = cmd.getOptionValue(CMD_OPT_HADOOP_USERNAME);

        String[] restOfArgs = cmd.getArgs();
        if (restOfArgs.length != 1) {
            System.out.println("Either backup or restore must be specified.");
            System.exit(-1);
            return;
        }

        Action action;
        switch (restOfArgs[0]) {
            case "backup":
                action = Action.BACKUP;
                break;
            case "restore":
                action = Action.RESTORE;
                break;
            default:
                System.out.println("Either backup or restore must be specified.");
                System.exit(-1);
                return;
        }

        switch (action) {
            case BACKUP:
                try {
                    String hdfsBackupDirectory = cmd.getOptionValue(CMD_OPT_HDFS_BACKUP_DIRECTORY, DEFAULT_HDFS_BACKUP_DIRECTORY);

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
                    String hdfsRestoreTempDirectory = cmd.getOptionValue(CMD_OPT_HDFS_RESTORE_TEMP_DIRECTORY);

                    RestoreOptions restoreOptions = new RestoreOptions()
                            .setHdfsRestoreDirectory(hdfsRestoreDirectory)
                            .setHdfsRestoreTempDirectory(hdfsRestoreTempDirectory);
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
                .setAccumuloUserName(accumuloUsername)
                .setAccumuloPassword(accumuloPassword)
                .setZookeeperServers(zookeeperServers)
                .setHadoopFsDefaultFS(hadoopFsDefaultFs)
                .setHadoopDfsClientUseDatanodeHostname(hadoopDfsClientUseDatanodeHostname)
                .setHadoopUsername(hadoopUsername);
    }

    private CommandLine parseOptions(String[] args) {
        Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Print this help")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_TABLE_NAME_PREFIX)
                        .hasArg()
                        .withDescription("Prefix of tables to backup, default: " + DEFAULT_TABLE_NAME_PREFIX)
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
                        .withLongOpt(CMD_OPT_ACCUMULO_USERNAME)
                        .hasArg()
                        .withDescription("Accumulo username")
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
                        .withDescription("Comma separated list of Zookeeper servers")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_BACKUP_DIRECTORY)
                        .hasArg()
                        .withDescription("Path in HDFS to backup files to, default: " + DEFAULT_HDFS_BACKUP_DIRECTORY)
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_RESTORE_DIRECTORY)
                        .hasArg()
                        .withDescription("Path in HDFS to restore backups files from")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HDFS_RESTORE_TEMP_DIRECTORY)
                        .hasArg()
                        .withDescription("Path in HDFS to duplicate backup files to before restoring so that the original backup can be restored again")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_HADOOP_FS_DEFAULT_FS)
                        .hasArg()
                        .withDescription("fs.defaultFS, e.g. hdfs://namenode:8020")
                        .create()
        );

        options.addOption(
                OptionBuilder
                .withLongOpt(CMD_OPT_HADOOP_DFS_CLIENT_USE_DATANODE_HOSTNAME)
                .withDescription("Equivalent to -Ddfs.client.use.datanode.hostname=true")
                .create()
        );

        options.addOption(
                OptionBuilder
                .withLongOpt(CMD_OPT_HADOOP_USERNAME)
                .hasArg()
                .withDescription("Username to send when interacting with HDFS")
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
