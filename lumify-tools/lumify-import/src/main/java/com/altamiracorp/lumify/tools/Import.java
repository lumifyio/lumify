package com.altamiracorp.lumify.tools;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.ingest.FileImport;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;

public class Import extends CommandLineBase {
    private static final String CMD_OPT_DATADIR = "datadir";
    private static final String CMD_OPT_QUEUE_DUPLICATES = "queuedups";
    private static final String CMD_OPT_VISIBILITY_SOURCE = "visibilitysource";
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

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_VISIBILITY_SOURCE)
                        .withDescription("The visibility source data.")
                        .hasArg()
                        .create()
        );

        return opts;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        File dataDir = new File(cmd.getOptionValue(CMD_OPT_DATADIR));
        boolean queueDuplicates = cmd.hasOption(CMD_OPT_QUEUE_DUPLICATES);
        String visibilitySource = cmd.getOptionValue(CMD_OPT_VISIBILITY_SOURCE, "");
        fileImport.importDirectory(dataDir, queueDuplicates, visibilitySource, null, getAuthorizations());
        return 0;
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }
}
