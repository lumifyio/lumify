package io.lumify.core.bootstrap.lib;

import java.io.IOException;
import java.net.URL;
import java.net.URLStreamHandler;

import org.apache.hadoop.conf.Configuration;

/**
 * The code for this class was copied form org.apache.hadoop.fs.FsUrlStreamHandler
 * and adapted to allow for passing the HDFS user.
 */
class HdfsUrlStreamHandler extends URLStreamHandler {

    private final String user;
    private Configuration conf;

    HdfsUrlStreamHandler(Configuration conf, String user) {
        this.conf = conf;
        this.user = user;
    }

    @Override
    protected HdfsUrlConnection openConnection(URL url) throws IOException {
        return new HdfsUrlConnection(conf, url, user);
    }

}
