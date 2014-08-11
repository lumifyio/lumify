package io.lumify.core.bootstrap.lib;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

/**
 * The code for this class was copied form org.apache.hadoop.fs.FsUrlConnection
 * and adapted to allow for passing the HDFS user.
 */
class HdfsUrlConnection extends URLConnection {

    private final String user;
    private Configuration conf;
    private InputStream is;

    HdfsUrlConnection(Configuration conf, URL url, String user) {
        super(url);
        this.conf = conf;
        this.user = user;
    }

    @Override
    public void connect() throws IOException {
        try {
            FileSystem fs = FileSystem.get(url.toURI(), conf, user);
            is = fs.open(new Path(url.getPath()));
        } catch (URISyntaxException e) {
            throw new IOException(e);
        } catch (InterruptedException e) {
            throw new IOException(e);
        }
    }

    /* @inheritDoc */
    @Override
    public InputStream getInputStream() throws IOException {
        if (is == null) {
            connect();
        }
        return is;
    }

}
