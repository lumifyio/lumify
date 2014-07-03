package io.lumify.core.util;

import java.io.*;

import static org.securegraph.util.CloseableUtils.closeQuietly;

/**
 * Helper class to handle Runtime.exec() output.
 */
public class StreamHelper extends Thread {

    private InputStream inputStream;
    private OutputStream outputStream;
    protected StringBuffer contentBuffer = null;

    protected String prefix = null;

    /**
     * the output writer
     */
    protected PrintWriter writer = null;

    /**
     * Append messages to this logger
     */
    protected LumifyLogger logger = null;

    /**
     * True to keep reading the streams
     */
    boolean keepReading = true;

    public StreamHelper(InputStream inputStream, LumifyLogger logger, String prefix) {
        this(inputStream, null, logger, null, prefix);
    }

    /**
     * Creates a new stream helper and immediately starts capturing output from
     * the given stream. Output will be captured to the given buffer and also
     * redirected to the provided output stream.
     *
     * @param inputStream   the input stream to read from
     * @param redirect      a stream to also redirect the captured output to
     * @param logger        the logger to append to
     * @param contentBuffer the buffer to write the captured output to
     */
    public StreamHelper(InputStream inputStream, OutputStream redirect,
                        LumifyLogger logger, StringBuffer contentBuffer, String prefix) {
        this.inputStream = inputStream;
        this.outputStream = redirect;
        this.logger = logger;
        this.contentBuffer = contentBuffer;
        this.prefix = prefix;
    }

    /**
     * Thread run
     */
    @Override
    public void run() {
        BufferedReader reader = null;
        InputStreamReader isreader = null;
        try {
            if (outputStream != null) {
                writer = new PrintWriter(outputStream);
            }
            isreader = new InputStreamReader(inputStream);
            reader = new BufferedReader(isreader);
            String line;
            while (keepReading && (line = reader.readLine()) != null) {
                if (prefix != null) {
                    line = prefix + line;
                }
                append(line);
                log(line);
            }
            if (writer != null)
                writer.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            closeQuietly(reader);
            closeQuietly(isreader);
            closeQuietly(writer);
        }
    }

    /**
     * This method will write any output from the stream to the the content buffer
     * and the logger.
     *
     * @param output the stream output
     */
    protected void append(String output) {
        // Process stream redirects
        if (writer != null) {
            writer.println(output);
        }

        // Fill the content buffer, if one has been assigned
        if (contentBuffer != null) {
            contentBuffer.append(output.trim());
            contentBuffer.append('\n');
        }

        // Append output to logger?
    }

    /**
     * If a logger has been specified, the output is written to the logger using
     * the defined log level.
     *
     * @param output the stream output
     */
    protected void log(String output) {
        if (logger != null) {
            logger.info("%s", output);
        }
    }
}
