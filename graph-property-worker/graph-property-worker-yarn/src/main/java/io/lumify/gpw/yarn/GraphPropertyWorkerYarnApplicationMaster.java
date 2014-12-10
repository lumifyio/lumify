package io.lumify.gpw.yarn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.protocolrecords.AllocateResponse;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.AMRMClient;
import org.apache.hadoop.yarn.client.api.NMClient;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.IOException;
import java.util.*;

public class GraphPropertyWorkerYarnApplicationMaster {
    @Parameter(names = {"-memory", "-mem"}, description = "Memory for each process in MB.")
    private int memory = 512;

    @Parameter(names = {"-cores"}, description = "Number of virtual cores each process uses.")
    private int virtualCores = 1;

    @Parameter(names = {"-instances", "-i"}, description = "Number of instances to start.")
    private int instances = 1;

    @Parameter(names = {"-appname"}, description = "App name.")
    private String appName = GraphPropertyWorkerYarnClient.DEFAULT_APP_NAME;

    @Parameter(names = {"-remotepath"}, description = "Path to the remote files.")
    private String remotePath = null;

    public static void main(String[] args) throws Exception {
        new GraphPropertyWorkerYarnApplicationMaster().run(args);
    }

    private void run(String[] args) throws Exception {
        System.out.println("BEGIN " + GraphPropertyWorkerYarnApplicationMaster.class.getName());
        new JCommander(this, args);

        if (remotePath == null) {
            throw new Exception("remotePath is required");
        }

        GraphPropertyWorkerYarnClient.printEnv();

        final String myClasspath = System.getProperty("java.class.path");

        final YarnConfiguration conf = new YarnConfiguration();
        final FileSystem fs = FileSystem.get(conf);
        final List<Path> resources = getResourceList(fs, new Path(remotePath));

        final StringBuilder classPathEnv = new StringBuilder(myClasspath);
        for (Path p : resources) {
            classPathEnv.append(':');
            classPathEnv.append(p.getName());
        }
        System.out.println("Classpath: " + classPathEnv);

        AMRMClient<AMRMClient.ContainerRequest> rmClient = createResourceManagerClient(conf);
        NMClient nmClient = createNodeManagerClient(conf);
        makeContainerRequests(rmClient);

        launchContainers(rmClient, nmClient, fs, resources, classPathEnv.toString());

        rmClient.unregisterApplicationMaster(FinalApplicationStatus.SUCCEEDED, "", "");
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

    private void launchContainers(AMRMClient<AMRMClient.ContainerRequest> rmClient, NMClient nmClient, FileSystem fs, List<Path> resources, String classPathEnv) throws YarnException, IOException, InterruptedException {
        int responseId = 0;
        int completedContainers = 0;
        while (completedContainers < instances) {
            AllocateResponse response = rmClient.allocate(responseId++);
            for (Container container : response.getAllocatedContainers()) {
                launchContainer(nmClient, container, fs, resources, classPathEnv);
            }
            for (ContainerStatus status : response.getCompletedContainersStatuses()) {
                completedContainers++;
                System.out.println("Completed container " + status.getContainerId());
            }
            Thread.sleep(100);
        }
    }

    private void launchContainer(NMClient nmClient, Container container, FileSystem fs, List<Path> resources, String classPathEnv) throws YarnException, IOException {
        ContainerLaunchContext ctx = Records.newRecord(ContainerLaunchContext.class);

        Map<String, LocalResource> localResources = createLocalResources(fs, resources);
        ctx.setLocalResources(localResources);

        String command = "${JAVA_HOME}/bin/java"
                + " -Xmx" + memory + "M"
                + " -Djava.net.preferIPv4Stack=true"
                + " -cp " + classPathEnv
                + " " + GraphPropertyWorkerYarnTask.class.getName()
                + " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
                + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr";
        System.out.println("Running: " + command);
        ctx.setCommands(Collections.singletonList(command));

        System.out.println("Launching container " + container.getId());
        nmClient.startContainer(container, ctx);
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

    private void makeContainerRequests(AMRMClient<AMRMClient.ContainerRequest> rmClient) {
        Priority priority = createPriorityRecord();
        Resource capability = createResourceRecord();

        for (int i = 0; i < instances; ++i) {
            AMRMClient.ContainerRequest containerAsk = new AMRMClient.ContainerRequest(capability, null, null, priority);
            System.out.println("Making res-req " + i);
            rmClient.addContainerRequest(containerAsk);
        }
    }

    private Resource createResourceRecord() {
        Resource capability = Records.newRecord(Resource.class);
        capability.setMemory(memory);
        capability.setVirtualCores(virtualCores);
        return capability;
    }

    private Priority createPriorityRecord() {
        Priority priority = Records.newRecord(Priority.class);
        priority.setPriority(0);
        return priority;
    }

    private NMClient createNodeManagerClient(YarnConfiguration conf) {
        NMClient nmClient = NMClient.createNMClient();
        nmClient.init(conf);
        nmClient.start();
        return nmClient;
    }

    private AMRMClient<AMRMClient.ContainerRequest> createResourceManagerClient(YarnConfiguration conf) throws IOException, YarnException {
        AMRMClient<AMRMClient.ContainerRequest> rmClient = AMRMClient.createAMRMClient();
        rmClient.init(conf);
        rmClient.start();

        rmClient.registerApplicationMaster("", 0, "");

        return rmClient;
    }
}
