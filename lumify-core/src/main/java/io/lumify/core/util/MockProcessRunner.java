package io.lumify.core.util;

import java.io.IOException;
import java.io.OutputStream;

public abstract class MockProcessRunner extends ProcessRunner {
    @Override
    public Process execute(String programName, String[] programArgs, OutputStream out, String logPrefix) throws IOException, InterruptedException {
        return onExecute(programName, programArgs, out);
    }

    protected abstract Process onExecute(String programName, String[] programArgs, OutputStream out) throws IOException;
}
