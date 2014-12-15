package io.lumify.lrp.yarn;

import io.lumify.yarn.ClientBase;

public class LongRunningProcessYarnClient extends ClientBase {
    public static final String APP_NAME = "long-running-process";

    public static void main(String[] args) throws Exception {
        new LongRunningProcessYarnClient().run(args);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected Class getApplicationMasterClass() {
        return LongRunningProcessYarnApplicationMaster.class;
    }
}
