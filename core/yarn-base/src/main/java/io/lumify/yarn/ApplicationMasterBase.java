package io.lumify.yarn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.client.api.async.AMRMClientAsync;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.util.*;

public abstract class ApplicationMasterBase implements AMRMClientAsync.CallbackHandler {
    @Parameter(names = {"-memory", "-mem"}, description = "Memory for each process in MB.")
    private int memory = 512;

    @Parameter(names = {"-cores"}, description = "Number of virtual cores each process uses.")
    private int virtualCores = 1;

    @Parameter(names = {"-instances", "-i"}, description = "Number of instances to start.")
    private int instances = 1;

    @Parameter(names = {"-appname"}, description = "App name.")
    private String appName = null;

    @Parameter(names = {"-remotepath"}, description = "Path to the remote files.")
    private String remotePath = null;

    private NMClient nmClient;
    private FileSystem fs;
    private List<Path> resources;
    private String classPathEnv;
    private int numContainersToWaitFor;
    private Priority priority;
    private Resource capability;
    private AMRMClientAsync<AMRMClient.ContainerRequest> rmClient;

    protected void run(String[] args) throws Exception {
        System.out.println("BEGIN " + this.getClass().getName());
        new JCommander(this, args);

        System.out.println("memory: " + memory);
        System.out.println("virtualCores: " + virtualCores);
        System.out.println("instances: " + instances);
        System.out.println("appName: " + appName);
        System.out.println("remotePath: " + remotePath);

        if (remotePath == null) {
            throw new Exception("remotePath is required");
        }

        ClientBase.printEnv();

        final String myClasspath = System.getProperty("java.class.path");

        final YarnConfiguration conf = new YarnConfiguration();
        fs = FileSystem.get(conf);
        resources = getResourceList(fs, new Path(remotePath));

        final StringBuilder classPathEnvBuilder = new StringBuilder(myClasspath);
        for (Path p : resources) {
            classPathEnvBuilder.append(':');
            classPathEnvBuilder.append(p.getName());
        }
        System.out.println("Classpath: " + classPathEnvBuilder);
        classPathEnv = classPathEnvBuilder.toString();

        nmClient = createNodeManagerClient(conf);

        rmClient = createResourceManagerClient(conf);
        rmClient.registerApplicationMaster("", 0, "");
        makeContainerRequests();

        System.out.println("[AM] waiting for containers to finish");
        while (!doneWithContainers()) {
            Thread.sleep(100);
        }

        rmClient.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "", "");
    }

    private boolean doneWithContainers() {
        return numContainersToWaitFor == 0;
    }

    private List<Path> getResourceList(FileSystem fs, Path remotePath) throws IOException {
        List<Path> resources = new ArrayList<Path>();
        RemoteIterator<LocatedFileStatus> files = fs.listFiles(remotePath, false);
        while (files.hasNext()) {
            LocatedFileStatus file = files.next();
            System.out.println("Adding local resource: " + file.getPath().toString());
            resources.add(file.getPath());
        }
        return resources;
    }

    private Map<String, LocalResource> createLocalResources(FileSystem fs, List<Path> resources) throws IOException {
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();
        for (Path p : resources) {
            FileStatus fileStatus = fs.getFileStatus(p);
            LocalResource rsc = LocalResource.newInstance(ConverterUtils.getYarnUrlFromURI(p.toUri()), LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, fileStatus.getLen(), fileStatus.getModificationTime());
            localResources.put(p.getName(), rsc);
        }
        return localResources;
    }

    private void makeContainerRequests() {
        numContainersToWaitFor = instances;
        for (int i = 0; i < instances; ++i) {
            System.out.println("Making res-req " + i);
            makeContainerRequest();
        }
    }

    private void makeContainerRequest() {
        ensureCreatePriorityRecord();
        ensureCreateResourceRecord();

        AMRMClient.ContainerRequest containerAsk = new AMRMClient.ContainerRequest(capability, null, null, priority);
        rmClient.addContainerRequest(containerAsk);
    }

    private void ensureCreateResourceRecord() {
        if (capability == null) {
            capability = Records.newRecord(Resource.class);
            capability.setMemory(memory);
            capability.setVirtualCores(virtualCores);
        }
    }

    private void ensureCreatePriorityRecord() {
        if (priority == null) {
            priority = Records.newRecord(Priority.class);
            priority.setPriority(0);
        }
    }

    private NMClient createNodeManagerClient(YarnConfiguration conf) {
        NMClient nmClient = NMClient.createNMClient();
        nmClient.init(conf);
        nmClient.start();
        return nmClient;
    }

    private AMRMClientAsync<AMRMClient.ContainerRequest> createResourceManagerClient(YarnConfiguration conf) throws IOException, YarnException {
        AMRMClientAsync<AMRMClient.ContainerRequest> rmClient = AMRMClientAsync.createAMRMClientAsync(100, this);
        rmClient.init(conf);
        rmClient.start();
        return rmClient;
    }

    @Override
    public void onContainersCompleted(List<ContainerStatus> statuses) {
        for (ContainerStatus status : statuses) {
            int exitStatus = status.getExitStatus();
            System.out.println("[AM] Completed container " + status.getContainerId() + " (return code: " + exitStatus + ")");
            if (exitStatus != ContainerExitStatus.SUCCESS) {
                System.out.println("[AM] Restarting failed process (return code: " + exitStatus + ")");
                makeContainerRequest();
            } else {
                synchronized (this) {
                    numContainersToWaitFor--;
                }
            }
        }
    }

    @Override
    public void onContainersAllocated(List<Container> containers) {
        for (Container container : containers) {
            try {
                launchContainer(container);
            } catch (Exception ex) {
                System.err.println("[AM] Error launching container " + container.getId() + " " + ex);
            }
        }
    }

    private void launchContainer(Container container) throws YarnException, IOException {
        ContainerLaunchContext ctx = Records.newRecord(ContainerLaunchContext.class);

        Map<String, LocalResource> localResources = createLocalResources(fs, resources);
        ctx.setLocalResources(localResources);

        String command = "${JAVA_HOME}/bin/java"
                + " -Xmx" + memory + "M"
                + " -Djava.net.preferIPv4Stack=true"
                + " -cp " + classPathEnv
                + " " + getTaskClass().getName()
                + " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
                + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr";
        System.out.println("Running: " + command);
        ctx.setCommands(Collections.singletonList(command));

        System.out.println("Launching container " + container.getId());
        nmClient.startContainer(container, ctx);
    }

    protected abstract Class getTaskClass();

    @Override
    public void onShutdownRequest() {

    }

    @Override
    public void onNodesUpdated(List<NodeReport> updatedNodes) {

    }

    @Override
    public float getProgress() {
        return 0;
    }

    @Override
    public void onError(Throwable e) {
        System.out.println("[AM] error " + e);
    }
}
