package io.lumify.tools;

import com.google.inject.Inject;
import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.ingest.FileImport;
import io.lumify.core.model.workspace.Workspace;
import io.lumify.core.model.workspace.WorkspaceRepository;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import java.io.File;

public class Import extends CommandLineBase {
    private static final String CMD_OPT_DATADIR = "datadir";
    private static final String CMD_OPT_QUEUE_DUPLICATES = "queuedups";
    private static final String CMD_OPT_VISIBILITY_SOURCE = "visibilitysource";
    private static final String CMD_OPT_WORKSPACE_ID = "workspaceid";
    private FileImport fileImport;
    private WorkspaceRepository workspaceRepository;

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

        opts.addOption(
                OptionBuilder
                        .withLongOpt(CMD_OPT_WORKSPACE_ID)
                        .withDescription("The workspace id to import the files into.")
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
        String workspaceId = cmd.getOptionValue(CMD_OPT_WORKSPACE_ID, null);
        Workspace workspace;
        if (workspaceId == null) {
            workspace = null;
        } else {
            workspace = workspaceRepository.findById(workspaceId, getUser());
        }
        fileImport.importDirectory(dataDir, queueDuplicates, visibilitySource, workspace, getUser(), getAuthorizations());
        return 0;
    }

    @Inject
    public void setFileImport(FileImport fileImport) {
        this.fileImport = fileImport;
    }

    @Inject
    public void setWorkspaceRepository(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
    }
}
