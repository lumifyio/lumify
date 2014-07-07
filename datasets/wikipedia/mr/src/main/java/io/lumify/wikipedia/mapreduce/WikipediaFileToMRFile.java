package io.lumify.wikipedia.mapreduce;

import io.lumify.core.util.LumifyLogger;
import io.lumify.core.util.LumifyLoggerFactory;
import io.lumify.wikipedia.RandomAccessFileInputStream;
import org.apache.commons.cli.*;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.text.DecimalFormat;

public class WikipediaFileToMRFile {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(WikipediaFileToMRFile.class);
    private static final DecimalFormat NUMBER_FORMATTER = new DecimalFormat("#,###");

    public static void main(String[] args) throws ParseException, IOException {
        Options options = new Options();

        options.addOption(
                OptionBuilder
                        .withLongOpt("in")
                        .withDescription("Input file name")
                        .hasArg(true)
                        .withArgName("file")
                        .create("i")
        );
        options.addOption(
                OptionBuilder
                        .withLongOpt("out")
                        .withDescription("Output file name")
                        .hasArg(true)
                        .withArgName("file")
                        .create("o")
        );
        options.addOption(
                OptionBuilder
                        .withLongOpt("help")
                        .withDescription("Prints help")
                        .hasArg(false)
                        .create("h")
        );

        CommandLineParser parser = new GnuParser();
        CommandLine cmd = parser.parse(options, args);

        if (cmd.hasOption("help")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("run", options, true);
            System.exit(1);
            return;
        }

        if (!cmd.hasOption("in")) {
            System.err.println("in is required");
            System.exit(1);
            return;
        }
        String in = cmd.getOptionValue("in");

        if (!cmd.hasOption("out")) {
            System.err.println("out is required");
            System.exit(1);
            return;
        }
        String out = cmd.getOptionValue("out");

        new WikipediaFileToMRFile().run(in, out);
    }

    private void run(String inputFileName, String outputFileName) throws IOException {
        InputStream in;
        RandomAccessFile randomAccessFile = null;

        File inputFile = new File(inputFileName);
        if (!inputFile.exists()) {
            throw new RuntimeException("Could not find " + inputFileName);
        }

        if (inputFile.getName().endsWith("bz2")) {
            FileInputStream fileInputStream = new FileInputStream(inputFile);
            in = new BZip2CompressorInputStream(fileInputStream);
        } else {
            randomAccessFile = new RandomAccessFile(inputFile, "r");
            in = new RandomAccessFileInputStream(randomAccessFile);
        }

        File outputFile = new File(outputFileName);
        if (outputFile.exists()) {
            throw new RuntimeException("Output file already exists " + outputFileName);
        }
        OutputStream out = new FileOutputStream(outputFile);

        run(randomAccessFile, in, out);
    }

    private void run(RandomAccessFile randomAccessFile, InputStream in, OutputStream out) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String line;
            int lineNumber = 0;
            int pageCount = 0;
            boolean foundStartPage = false;

            while ((line = reader.readLine()) != null) {
                if ((lineNumber % 100000) == 0) {
                    LOGGER.info("Processing line " + NUMBER_FORMATTER.format(lineNumber) + (randomAccessFile == null ? "" : " (offset: " + randomAccessFile.getFilePointer() + ")"));
                }
                if (line.contains("<page>") && line.trim().equals("<page>")) {
                    foundStartPage = true;
                    writeLine(line, out);
                } else if (line.contains("</page>") && line.trim().equals("</page>")) {
                    writeLine(line, out);
                    out.write("\n".getBytes());

                    pageCount++;
                    if ((pageCount % 1000) == 0) {
                        LOGGER.info("Processing page " + NUMBER_FORMATTER.format(pageCount));
                    }
                    foundStartPage = false;
                } else if (foundStartPage) {
                    writeLine(line, out);
                    out.write("\\n".getBytes());
                }
                lineNumber++;
            }
        } finally {
            reader.close();
        }
    }

    private void writeLine(String line, OutputStream out) throws IOException {
        out.write(line.trim().getBytes());
    }
}
