package com.altamiracorp.lumify.core.util;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

public class ProcessRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessRunner.class);

    public static void execute(final String programName, final String[] programArgs, OutputStream out) throws IOException, InterruptedException {
        final List<String> arguments = Lists.newArrayList(programName);
        for (String programArg : programArgs) {
            if (programArg == null) {
                throw new NullPointerException("Argument was null in argument list [ " + Joiner.on(", ").useForNull("null").join(programArgs) + " ]");
            }
            arguments.add(programArg);
        }

        final ProcessBuilder procBuilder = new ProcessBuilder(arguments);
        final Map<String, String> sortedEnv = new TreeMap<String, String>(procBuilder.environment());

        LOGGER.info("Running: " + arrayToString(arguments));

        if (!sortedEnv.isEmpty()) {
            LOGGER.info("Spawned program environment: ");
            for (final Map.Entry<String, String> entry : sortedEnv.entrySet()) {
                LOGGER.info(String.format("%s:%s", entry.getKey(), entry.getValue()));
            }
        } else {
            LOGGER.info("Running program environment is empty");
        }

        final Process proc = procBuilder.start();

        StreamHelper errStreamHelper = new StreamHelper(proc.getErrorStream(), LOGGER, programName + "(stderr): ");
        errStreamHelper.start();

        final Exception[] pipeException = new Exception[1];
        Pipe pipe = null;
        StreamHelper stdoutStreamHelper = null;
        if (out == null) {
            stdoutStreamHelper = new StreamHelper(proc.getInputStream(), LOGGER, programName + "(stdout): ");
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

        LOGGER.info(programName + "(returncode): " + proc.exitValue());

        if (proc.exitValue() != 0) {
            throw new RuntimeException("unexpected return code: " + proc.exitValue() + " for command " + arrayToString(arguments));
        }
        if (pipeException[0] != null) {
            throw new RuntimeException("pipe exception", pipeException[0]);
        }
    }

    private static String arrayToString(List<String> arr) {
        StringBuilder result = new StringBuilder();
        for (String s : arr) {
            result.append(s).append(' ');
        }
        return result.toString();
    }
}

