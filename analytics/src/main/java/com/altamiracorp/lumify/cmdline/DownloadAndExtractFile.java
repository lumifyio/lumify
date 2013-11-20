package com.altamiracorp.lumify.cmdline;


import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.accumulo.core.util.CachedConfiguration;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.hadoop.util.ToolRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
//import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;

public class DownloadAndExtractFile extends CommandLineBase {
    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadAndExtractFile.class.getName());
    private String directory;
    private String zipfile;
    private String storage;

    public static void main(String[] args) throws Exception {
        int res = ToolRunner.run(CachedConfiguration.getInstance(), new DownloadAndExtractFile(), args);
        if (res != 0) {
            System.exit(res);
        }
    }

    @Override
    protected int run(CommandLine cmd) throws Exception {
        if(zipfile == null ){
           if (!directoryExists(getDirectory())) {
                File file = new File(getDirectory());
                file.mkdirs();
                zipfile =  getZipfileName(getDirectory())+ ".zip";
            } else {
                return 0;
            }
        } else if (!zipfile.contains(".zip")){
            if(directory == null) {
                directory = "data/import/";
            }
            if(zipfile.contains("/")){
                zipfile =  getZipfileName(getDirectory());
            }
            zipfile += ".zip";
        }
        if (directory.charAt(directory.length() - 1) != '/') {
            directory += "/";
        }
        if (directory.endsWith("import/")){
            storage = directory.substring(0, (directory.length() - 7));
        } else {
            storage = directory;
            if(!directoryExists(storage)){
                File file = new File(getDirectory());
                file.mkdirs();
            }
        }
        LOGGER.info("using dataset: " + zipfile);
        downloadDataset();
        extractDataset();
        return 0;
    }
    @Override
    protected void processOptions(CommandLine cmd) throws Exception {
        super.processOptions(cmd);
        this.directory = (String) cmd.getArgList().get(0);
        this.zipfile = (String) cmd.getArgList().get(1);
        if (this.zipfile == null && this.directory == null) throw new RuntimeException("No dataset name provided to FileDownload");
    }

    @Override
    protected Options getOptions() {
        Options options = super.getOptions();
        options.addOption(
                OptionBuilder
                        .withArgName("d")
                        .withLongOpt("directory")
                        .withDescription("The directory to extract into")
                        .withArgName("path")
                        .create()
        );

        options.addOption(
                OptionBuilder
                        .withArgName("z")
                        .withLongOpt("zipfile")
                        .withDescription("The zip files to download and extract")
                        .withArgName("zipfile")
                        .create()
        );
        return options;
    }

    public String getDirectory() {
        return directory;
    }
    private Boolean directoryExists(String dir) {
        File file = new File(dir);
        return (file.exists() || (dir.equals("sample/directory")));
    }

    private String getZipfileName(String dir) {
        int slash = dir.lastIndexOf('/', (dir.length() - 3));
        String set;
        if (dir.charAt((dir.length()-1)) == '/') {
            int last = dir.lastIndexOf('/');
            set = dir.substring(slash, last);
        } else {
            set = dir.substring(slash);
            this.directory += "/";
        }
        return set;
    }

    private void downloadDataset() {
        try {
            String amazon = "https://s3.amazonaws.com/RedDawn/DataSets/";
            URL aws = new URL(amazon + zipfile);
            URLConnection connect;
            if(System.getenv("http_proxy") != null){
                String httpproxy = System.getenv("http_proxy");
                String protocol  = httpproxy.substring(0, httpproxy.lastIndexOf("/"));
                String ip        = httpproxy.substring((httpproxy.lastIndexOf("/")+1), httpproxy.lastIndexOf(":"));
                String port      = httpproxy.substring(httpproxy.lastIndexOf(":")+1);
                int portnumber = Integer.parseInt(port);
                if(!protocol.contains("s")){
                    Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, portnumber));
                    connect = aws.openConnection(proxy);
                } else {
                    connect = null;
                    LOGGER.info("Cannot create a secure proxy request.");
                }
            } else{
                connect = aws.openConnection();
            }
            InputStream in = connect.getInputStream();
            FileOutputStream out = new FileOutputStream(storage + zipfile);
            byte[] outStream = new byte[4096];
            int count;
            while ((count = in.read(outStream)) >= 0) {
                out.write(outStream, 0, count);
            }
            in.close();
            out.close();
        } catch (IOException e) {
            LOGGER.info("Dataset does not exist.  " +
                    "\nPlease choose from the following options:" +
                    "\n\t * bombing-100-docs" +
                    "\n\t * chicago-all" +
                    "\n\t * congress-250-all" +
                    "\n\t * election-100-images" +
                    "\n\t * fda-25-docs" +
                    "\n\t * fema" +
                    "\n\t * impacts-of-katrina" +
                    "\n\t * katrina-small" +
                    "\n\t * NBIC" +
                    "\n\t * new-york" +
                    "\n\t * pope-25-all" +
                    "\n\t * quotes-25-images" +
                    "\n\t * sandy-500-all" +
                    "\n\t * sample-graph-dataset" +
                    "\n\t * tucson-100-all" +
                    "\n\t * video");
            e.printStackTrace();
        }
    }

    private void extractDataset() {
       try {
        ZipFile zipped = new ZipFile(storage + zipfile);
        zipped.extractAll(directory);
       } catch (ZipException e) {
            LOGGER.info("Error in extracting data from zip file");
            e.printStackTrace();
       }
    }
}
