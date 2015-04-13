package io.lumify.gpw.yarn;

import io.lumify.yarn.ClientBase;

public class GraphPropertyWorkerYarnClient extends ClientBase {
    public static final String APP_NAME = "graph-property-worker";

    public static void main(String[] args) throws Exception {
        new GraphPropertyWorkerYarnClient().run(args);
    }

    @Override
    protected String getAppName() {
        return APP_NAME;
    }

    @Override
    protected Class getApplicationMasterClass() {
        return GraphPropertyWorkerYarnApplicationMaster.class;
    }
}
