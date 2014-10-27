package io.lumify.palantir.dataImport;

import io.lumify.core.cmdline.CommandLineBase;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.Authorizations;
import org.securegraph.Visibility;

public class DataImport extends CommandLineBase {
    private static final String CMD_OPT_TABLE_NAMESPACE = "namespace";
    private static final String CMD_OPT_CONNECTION_STRING = "connectionstring";
    private static final String CMD_OPT_USERNAME = "username";
    private static final String CMD_OPT_PASSWORD = "password";
    private static final String CMD_OPT_ID_PREFIX = "idPrefix";
    private static final String CMD_OPT_OWL_PREFIX = "owlprefix";
    private static final String CMD_OPT_OUT_DIR = "outdir";

    public static void main(String[] args) throws Exception {
        int res = new DataImport().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_TABLE_NAMESPACE)
                        .withDescription("Table namespace")
                        .hasArg()
                        .isRequired()
                        .create("n")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_CONNECTION_STRING)
                        .withDescription("Database connection string")
                        .hasArg()
                        .isRequired()
                        .create("cs")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_USERNAME)
                        .withDescription("Username")
                        .hasArg()
                        .isRequired()
                        .create("u")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_PASSWORD)
                        .withDescription("Password")
                        .hasArg()
                        .isRequired()
                        .create("p")
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_OWL_PREFIX)
                        .withDescription("URI prefix used when converting the ontology")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_ID_PREFIX)
                        .withDescription("Prefix of lumify ids")
                        .hasArg()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_OUT_DIR)
                        .withDescription("Output directory for ontology items")
                        .hasArg()
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String tableNamespace = cmd.getOptionValue(CMD_OPT_TABLE_NAMESPACE);
        String connectionString = cmd.getOptionValue(CMD_OPT_CONNECTION_STRING);
        String username = cmd.getOptionValue(CMD_OPT_USERNAME);
        String password = cmd.getOptionValue(CMD_OPT_PASSWORD);
        String idPrefix = cmd.getOptionValue(CMD_OPT_ID_PREFIX, "palantir");
        String outdir = cmd.getOptionValue(CMD_OPT_OUT_DIR);
        String owlPrefix = cmd.getOptionValue(CMD_OPT_OWL_PREFIX);
        Visibility visibility = new Visibility("");
        Authorizations authorizations = getAuthorizations();

        new DataImporter(connectionString, username, password, tableNamespace, idPrefix, owlPrefix, outdir, getGraph(), visibility, authorizations).run();

        return 0;
    }
}
