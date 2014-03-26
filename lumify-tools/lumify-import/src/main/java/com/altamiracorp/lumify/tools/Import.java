package com.altamiracorp.lumify.tools;

import com.altamiracorp.lumify.core.cmdline.CommandLineBase;
import com.altamiracorp.lumify.core.model.properties.LumifyProperties;
import com.altamiracorp.lumify.core.model.properties.RawLumifyProperties;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.RowKeyHelper;
import com.altamiracorp.securegraph.Graph;
import com.altamiracorp.securegraph.Vertex;
import com.altamiracorp.securegraph.VertexBuilder;
import com.altamiracorp.securegraph.Visibility;
import com.altamiracorp.securegraph.mutation.ElementMutation;
import com.altamiracorp.securegraph.property.StreamingPropertyValue;
import com.google.inject.Inject;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;

public class Import extends CommandLineBase {
    private static final String CMD_OPT_DATADIR = "datadir";
    private static final String CMD_OPT_QUEUE_DUPLICATES = "queuedups";
    private Graph graph;
    private WorkQueueRepository workQueueRepository;

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

        LOGGER.debug("Importing files from %s", dataDir);
        File dataDirFile = new File(dataDir);

        int totalFileCount = dataDirFile.listFiles().length;
        int fileCount = 0;
        int importedFileCount = 0;
        try {
            for (File f : dataDirFile.listFiles()) {
                if (f.getName().startsWith(".") || f.length() == 0) {
                    continue;
                }

                LOGGER.debug("Importing file (%d/%d): %s", fileCount + 1, totalFileCount, f.getAbsolutePath());
                if (importFile(f, queueDuplicates)) {
                    importedFileCount++;
                }
                fileCount++;
            }
        } finally {
            graph.flush();
        }

        LOGGER.debug(String.format("Imported %d, skipped %d files from %s", importedFileCount, fileCount - importedFileCount, dataDirFile));
        return 0;
    }

    private boolean importFile(File f, boolean queueDuplicates) throws IOException {
        String hash = calculateFileHash(f);

        Vertex vertex = findExistingVertexWithHash(hash);
        if (vertex != null) {
            LOGGER.warn("vertex already exists with hash %s", hash);
            if (queueDuplicates) {
                pushOnQueue(vertex);
            }
            return false;
        }

        FileInputStream fileInputStream = new FileInputStream(f);
        try {
            StreamingPropertyValue rawValue = new StreamingPropertyValue(fileInputStream, byte[].class);
            rawValue.searchIndex(false);

            Visibility visibility = new Visibility("");
            VertexBuilder vertexBuilder = this.graph.prepareVertex(visibility, getAuthorizations());
            RawLumifyProperties.RAW.setProperty(vertexBuilder, rawValue, visibility);
            LumifyProperties.TITLE.setProperty(vertexBuilder, f.getName(), visibility);
            LumifyProperties.ROW_KEY.setProperty(vertexBuilder, hash, visibility);
            RawLumifyProperties.FILE_NAME.setProperty(vertexBuilder, f.getName(), visibility);
            RawLumifyProperties.FILE_NAME_EXTENSION.setProperty(vertexBuilder, FilenameUtils.getExtension(f.getName()), visibility);
            vertex = vertexBuilder.save();
            graph.flush();
            LOGGER.debug("File %s imported. vertex id: %s", f.getAbsolutePath(), vertex.getId().toString());
            pushOnQueue(vertex);
            return true;
        } finally {
            fileInputStream.close();
        }
    }

    private void pushOnQueue(Vertex vertex) {
        LOGGER.debug("pushing %s on to %s queue", vertex.getId().toString(), WorkQueueRepository.GRAPH_PROPERTY_QUEUE_NAME);
        this.workQueueRepository.pushGraphPropertyQueue(vertex.getId(), ElementMutation.DEFAULT_KEY, RawLumifyProperties.RAW.getKey());
    }

    private Vertex findExistingVertexWithHash(String hash) {
        Iterator<Vertex> existingVertices = this.graph.query(getAuthorizations())
                .has(LumifyProperties.ROW_KEY.getKey(), hash)
                .vertices()
                .iterator();
        if (existingVertices.hasNext()) {
            return existingVertices.next();
        }
        return null;
    }

    private String calculateFileHash(File f) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(f);
        try {
            return RowKeyHelper.buildSHA256KeyString(fileInputStream);
        } finally {
            fileInputStream.close();
        }
    }

    @Inject
    public void setGraph(Graph graph) {
        this.graph = graph;
    }

    @Inject
    public void setWorkQueueRepository(WorkQueueRepository workQueueRepository) {
        this.workQueueRepository = workQueueRepository;
    }
}
