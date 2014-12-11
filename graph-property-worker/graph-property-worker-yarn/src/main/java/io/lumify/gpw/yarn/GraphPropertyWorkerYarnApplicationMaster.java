package io.lumify.gpw.yarn;

import io.lumify.yarn.ApplicationMasterBase;

public class GraphPropertyWorkerYarnApplicationMaster extends ApplicationMasterBase {
    public static void main(String[] args) throws Exception {
        new GraphPropertyWorkerYarnApplicationMaster().run(args);
    }

    @Override
    protected Class getTaskClass() {
        return GraphPropertyWorkerYarnTask.class;
    }
}
