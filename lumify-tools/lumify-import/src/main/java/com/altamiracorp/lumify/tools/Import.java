package com.altamiracorp.lumify.tools;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.ingest.FileImport;
import com.altamiracorp.securegraph.Visibility;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

public class Import extends CommandLineBase {
    private static final String CMD_OPT_DATADIR = "datadir";
    private static final String CMD_OPT_QUEUE_DUPLICATES = "queuedups";
    private FileImport fileImport;

    public static void main(String[] args) throws Exception {
        int res = new Import().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options opts = super.getOptions();

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_DATADIR)
                        .withDescription("Location of the data directory")
                        .hasArg()
                        .isRequired()
                        .create()
        );

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_QUEUE_DUPLICATES)
                        .withDescription("Specify if you would like to queue duplicate files")
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String dataDir = cmd.getOptionValue(CMD_OPT_DATADIR);
        boolean queueDuplicates = cmd.hasOption(CMD_OPT_QUEUE_DUPLICATES);
        Visibility visibility = new Visibility("");
        fileImport.importDirectory(dataDir, queueDuplicates, visibility, getAuthorizations());
        return 0;
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }
}
