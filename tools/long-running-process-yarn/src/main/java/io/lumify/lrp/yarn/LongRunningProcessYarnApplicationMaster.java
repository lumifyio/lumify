package io.lumify.lrp.yarn;

import io.lumify.yarn.ApplicationMasterBase;

public class LongRunningProcessYarnApplicationMaster extends ApplicationMasterBase {
    public static void main(String[] args) throws Exception {
        new LongRunningProcessYarnApplicationMaster().run(args);
    }

    @Override
    protected Class getTaskClass() {
        return LongRunningProcessYarnTask.class;
    }
}
