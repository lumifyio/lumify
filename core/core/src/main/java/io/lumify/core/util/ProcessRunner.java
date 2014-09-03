package io.lumify.core.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(ProcessRunner.class);

    public Process execute(final String programName, final String[] programArgs, OutputStream out, final String logPrefix) throws IOException, InterruptedException {
        final List<String> arguments = Lists.newArrayList(programName);
        for (String programArg : programArgs) {
            if (programArg == null) {
                throw new NullPointerException("Argument was null in argument list [ " + Joiner.on(", ").useForNull("null").join(programArgs) + " ]");
            }
            arguments.add(programArg);
        }

        final ProcessBuilder procBuilder = new ProcessBuilder(arguments);
        final Map<String, String> sortedEnv = new TreeMap<String, String>(procBuilder.environment());

        LOGGER.info("%s Running: %s", logPrefix, arrayToString(arguments));

        if (!sortedEnv.isEmpty()) {
            LOGGER.info("%s Spawned program environment: ", logPrefix);
            for (final Map.Entry<String, String> entry : sortedEnv.entrySet()) {
                LOGGER.info("%s %s:%s", logPrefix, entry.getKey(), entry.getValue());
            }
        } else {
            LOGGER.info("%s Running program environment is empty", logPrefix);
        }

        final Process proc = procBuilder.start();

        StreamHelper errStreamHelper = new StreamHelper(proc.getErrorStream(), LOGGER, logPrefix + programName + "(stderr): ");
        errStreamHelper.start();

        final Exception[] pipeException = new Exception[1];
        Pipe pipe = null;
        StreamHelper stdoutStreamHelper = null;
        if (out == null) {
            stdoutStreamHelper = new StreamHelper(proc.getInputStream(), LOGGER, logPrefix + programName + "(stdout): ");
            stdoutStreamHelper.start();
        } else {
            Pipe.StatusHandler statusHandler = new Pipe.StatusHandler() {
                @Override
                public void handleException(Exception e) {
                    pipeException[0] = e;
                }

            };
            pipe = new Pipe().pipe(proc.getInputStream(), out, statusHandler);
        }

        // Pipe will ensure to some degree that we have started reading but if the process exits at
        //  just the right time (after threadStarted = true but before the in.read occurs) we could miss the output
        //  this sleep should be enough to prevent this happening.
        Thread.sleep(100);

        proc.waitFor();

        errStreamHelper.join(10000);

        if (stdoutStreamHelper != null) {
            stdoutStreamHelper.join(10000);
        }

        if (pipe != null) {
            pipe.waitForCompletion(10000, TimeUnit.MILLISECONDS);
        }

        proc.getOutputStream().close(); // stdin
        proc.getInputStream().close(); // stdout
        proc.getErrorStream().close();

        LOGGER.info(logPrefix + programName + "(returncode): " + proc.exitValue());

        if (proc.exitValue() != 0) {
            throw new RuntimeException("unexpected return code: " + proc.exitValue() + " for command " + arrayToString(arguments));
        }
        if (pipeException[0] != null) {
            throw new RuntimeException("pipe exception", pipeException[0]);
        }

        return proc;
    }

    private static String arrayToString(List<String> arr) {
        StringBuilder result = new StringBuilder();
        for (String s : arr) {
            result.append(s).append(' ');
        }
        return result.toString();
    }
}

