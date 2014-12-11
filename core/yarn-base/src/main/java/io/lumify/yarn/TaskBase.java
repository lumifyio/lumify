package io.lumify.yarn;

import com.beust.jcommander.JCommander;

public abstract class TaskBase {
    public final void run(String[] args) {
        new JCommander(this, args);
        run();
    }

    protected abstract void run();
}
