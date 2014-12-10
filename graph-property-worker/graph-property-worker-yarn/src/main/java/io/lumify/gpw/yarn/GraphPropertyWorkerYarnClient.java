package io.lumify.gpw.yarn;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.yarn.api.ApplicationConstants;
import org.apache.hadoop.yarn.api.records.*;
import org.apache.hadoop.yarn.client.api.YarnClient;
import org.apache.hadoop.yarn.client.api.YarnClientApplication;
import org.apache.hadoop.yarn.conf.YarnConfiguration;
import org.apache.hadoop.yarn.exceptions.YarnException;
import org.apache.hadoop.yarn.util.ConverterUtils;
import org.apache.hadoop.yarn.util.Records;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GraphPropertyWorkerYarnClient {
    public static final String DEFAULT_APP_NAME = "graph-property-worker";

    @Parameter(names = {"-memory", "-mem"}, description = "Memory for each process in MB.")
    private int memory = 512;

    @Parameter(names = {"-cores"}, description = "Number of virtual cores each process uses.")
    private int virtualCores = 1;

    @Parameter(names = {"-instances", "-i"}, description = "Number of instances to start.")
    private int instances = 1;

    @Parameter(names = {"-appname"}, description = "App name.")
    private String appName = DEFAULT_APP_NAME;

    @Parameter(names = {"-jar"}, description = "Path to jar.")
    private String jar = null;

    public static void main(String[] args) throws Exception {
        new GraphPropertyWorkerYarnClient().run(args);
    }

    protected int run(String[] args) throws Exception {
        new JCommander(this, args);

        printEnv();

        final String myClasspath = System.getProperty("java.class.path");
        final String localResourceJarFileName = appName + ".jar";

        final File jarPath = new File(jar);
        if (!jarPath.isFile()) {
            throw new Exception("YARN app must be packaged as a jar file (found path: " + jarPath + ").");
        }
        System.out.println("Using jar path: " + jarPath);

        final String classPathEnv = myClasspath + ":./" + localResourceJarFileName;
        System.out.println("Classpath: " + classPathEnv);

        final YarnConfiguration conf = new YarnConfiguration();
        final FileSystem fs = FileSystem.get(conf);
        final Path remotePath = new Path(fs.getHomeDirectory(), appName);

        final YarnClient yarnClient = createYarnClient(conf);
        final YarnClientApplication app = yarnClient.createApplication();
        final ContainerLaunchContext amContainer = createContainerLaunchContextRecord(classPathEnv, remotePath);
        final Resource capability = createResourceRecord();
        final ApplicationSubmissionContext appContext = createApplicationSubmissionContext(app, amContainer, capability);
        final ApplicationId appId = appContext.getApplicationId();
        amContainer.setLocalResources(createLocalResources(fs, remotePath, localResourceJarFileName, jarPath));
        amContainer.setEnvironment(createEnvironment(classPathEnv));

        System.out.println("Submitting application " + appId);
        yarnClient.submitApplication(appContext);

        waitForApplication(yarnClient, appId, 30, TimeUnit.SECONDS);

        return 0;
    }

    private void waitForApplication(YarnClient yarnClient, ApplicationId appId, int time, TimeUnit timeUnit) throws YarnException, IOException, InterruptedException {
        Date startTime = new Date();
        Date endTime = new Date(startTime.getTime() + timeUnit.toMillis(time));
        ApplicationReport appReport = yarnClient.getApplicationReport(appId);
        YarnApplicationState appState = appReport.getYarnApplicationState();
        while (appState != YarnApplicationState.FINISHED &&
                appState != YarnApplicationState.KILLED &&
                appState != YarnApplicationState.FAILED) {
            if (System.currentTimeMillis() > endTime.getTime()) {
                break;
            }
            Thread.sleep(100);
            appReport = yarnClient.getApplicationReport(appId);
            appState = appReport.getYarnApplicationState();
        }

        System.out.println("Application " + appId + " state " + appState);
    }

    private Map<String, String> createEnvironment(String classPathEnv) {
        Map<String, String> appMasterEnv = new HashMap<String, String>();
        appMasterEnv.putAll(System.getenv());
        appMasterEnv.put(ApplicationConstants.Environment.CLASSPATH.name(), classPathEnv);
        return appMasterEnv;
    }

    private Map<String, LocalResource> createLocalResources(FileSystem fs, Path remotePath, String localResourceJarFileName, File jarPath) throws IOException {
        Map<String, LocalResource> localResources = new HashMap<String, LocalResource>();
        addToLocalResources(fs, remotePath, jarPath.getPath(), localResourceJarFileName, localResources, null);
        return localResources;
    }

    private YarnClient createYarnClient(YarnConfiguration conf) {
        YarnClient yarnClient = YarnClient.createYarnClient();
        yarnClient.init(conf);
        yarnClient.start();
        return yarnClient;
    }

    private Resource createResourceRecord() {
        Resource capability = Records.newRecord(Resource.class);
        capability.setMemory(memory);
        capability.setVirtualCores(virtualCores);
        return capability;
    }

    private ContainerLaunchContext createContainerLaunchContextRecord(String classPathEnv, Path remotePath) {
        ContainerLaunchContext amContainer = Records.newRecord(ContainerLaunchContext.class);
        String command = "${JAVA_HOME}/bin/java"
                + " -Xmx" + memory + "M"
                + " -Djava.net.preferIPv4Stack=true"
                + " -cp " + classPathEnv
                + " " + GraphPropertyWorkerYarnApplicationMaster.class.getName()
                + " -memory " + memory
                + " -cores " + virtualCores
                + " -instances " + instances
                + " -appname " + appName
                + " -remotepath " + remotePath
                + " 1>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stdout"
                + " 2>" + ApplicationConstants.LOG_DIR_EXPANSION_VAR + "/stderr";
        System.out.println("Running: " + command);
        amContainer.setCommands(Collections.singletonList(command));
        return amContainer;
    }

    private ApplicationSubmissionContext createApplicationSubmissionContext(YarnClientApplication app, ContainerLaunchContext amContainer, Resource capability) {
        final ApplicationSubmissionContext appContext = app.getApplicationSubmissionContext();
        appContext.setApplicationName(appName);
        appContext.setAMContainerSpec(amContainer);
        appContext.setResource(capability);
        appContext.setQueue("default");
        return appContext;
    }

    private void addToLocalResources(FileSystem fs, Path remotePath, String fileSrcPath, String fileDstPath, Map<String, LocalResource> localResources, String resources) throws IOException {
        Path dst = new Path(remotePath, fileDstPath);
        if (fileSrcPath == null) {
            FSDataOutputStream ostream = null;
            try {
                ostream = FileSystem.create(fs, dst, new FsPermission((short) 0710));
                ostream.writeUTF(resources);
            } finally {
                IOUtils.closeQuietly(ostream);
            }
        } else {
            fs.copyFromLocalFile(new Path(fileSrcPath), dst);
        }
        FileStatus scFileStatus = fs.getFileStatus(dst);
        LocalResource scRsrc = LocalResource.newInstance(ConverterUtils.getYarnUrlFromURI(dst.toUri()), LocalResourceType.FILE, LocalResourceVisibility.APPLICATION, scFileStatus.getLen(), scFileStatus.getModificationTime());
        localResources.put(fileDstPath, scRsrc);
    }

    public static void printEnv() {
        System.out.println("Environment:");
        for (Map.Entry<String, String> e : System.getenv().entrySet()) {
            System.out.println("  " + e.getKey() + "=" + e.getValue());
        }
    }
}
