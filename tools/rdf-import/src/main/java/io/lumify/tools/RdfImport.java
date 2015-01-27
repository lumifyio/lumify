package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.exception.LumifyException;
import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.rdf.RdfGraphPropertyWorker;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.Visibility;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

public class RdfImport extends CommandLineBase {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(RdfImport.class);

    public static void main(String[] args) throws Exception {
        int res = new RdfImport().run(args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();

        options.addOption(
                OptionBuilder
                        .withLongOpt("infile")
                        .withDescription("Input file")
                        .hasArg(true)
                        .withArgName("filename")
                        .create("i")
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("indir")
                        .withDescription("Input directory")
                        .hasArg(true)
                        .withArgName("dir")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withLongOpt("pattern")
                        .withDescription("Input directory pattern [default: *.{xml}]")
                        .hasArg(true)
                        .withArgName("pattern")
                        .create()
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String[] inputFileNames = cmd.getOptionValues("infile");
        importInFiles(inputFileNames);

        String[] inputDirs = cmd.getOptionValues("indir");
        String pattern = cmd.getOptionValue("pattern");
        importInDirs(inputDirs, pattern);
        return 0;
    }

    private void importInDirs(String[] inputDirs, String pattern) throws IOException {
        if (pattern == null) {
            pattern = "*.{xml}";
        }
        if (inputDirs != null) {
            for (String inputDirName : inputDirs) {
                File inputDir = new File(inputDirName);
                if (!inputDir.exists()) {
                    throw new LumifyException("Could not find input directory: " + inputDir.getAbsolutePath());
                }
                PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
                File[] files = inputDir.listFiles();
                if (files == null) {
                    continue;
                }
                for (File inputFile : files) {
                    Path fileNamePath = FileSystems.getDefault().getPath(inputFile.getName());
                    if (matcher.matches(fileNamePath)) {
                        importFile(inputFile);
                    }
                }
            }
        }
    }

    private void importInFiles(String[] inputFileNames) throws IOException {
        if (inputFileNames != null) {
            for (String inputFileName : inputFileNames) {
                File inputFile = new File(inputFileName);
                if (!inputFile.exists()) {
                    throw new LumifyException("Could not find file: " + inputFile.getAbsolutePath());
                }
                importFile(inputFile);
            }
        }
    }

    private void importFile(File inputFile) throws IOException {
        LOGGER.info("Importing file: %s", inputFile.getAbsolutePath());
        RdfGraphPropertyWorker rdfGraphPropertyWorker = new RdfGraphPropertyWorker();
        Visibility visibility = new Visibility("");
        rdfGraphPropertyWorker.importRdf(getGraph(), inputFile, null, visibility, getAuthorizations());
        getGraph().flush();
    }
}
