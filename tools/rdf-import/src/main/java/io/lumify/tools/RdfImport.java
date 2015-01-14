package io.lumify.tools;

import io.lumify.core.cmdline.CommandLineBase;
import io.lumify.core.exception.LumifyException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.securegraph.Visibility;

import java.io.File;

public class RdfImport extends CommandLineBase {
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
                        .withLongOpt("in")
                        .withDescription("Input file")
                        .isRequired()
                        .hasArg(true)
                        .withArgName("filename")
                        .create("i")
        );

        return options;
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        String inputFileName = cmd.getOptionValue("in");
        File inputFile = new File(inputFileName);
        if (!inputFile.exists()) {
            throw new LumifyException("Could not find file: " + inputFileName);
        }

        RdfGraphPropertyWorker rdfGraphPropertyWorker = new RdfGraphPropertyWorker();

        Visibility visibility = new Visibility("");
        rdfGraphPropertyWorker.importRdf(getGraph(), inputFile, null, visibility, getAuthorizations());
        getGraph().flush();

        return 0;
    }
}
