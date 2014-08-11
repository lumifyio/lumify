package io.lumify.core.bootstrap.lib;

import java.net.URLStreamHandlerFactory;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;

/**
 * The code for this class was copied form org.apache.hadoop.fs.FsUrlStreamHandlerFactory
 * and adapted to allow for passing the HDFS user.
 */
public class HdfsUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    // The configuration holds supported FS implementation class names.
    private Configuration conf;

    // This map stores whether a protocol is know or not by FileSystem
    private Map<String, Boolean> protocols = new HashMap<String, Boolean>();

    // The URL Stream handler
    private java.net.URLStreamHandler handler;

    public HdfsUrlStreamHandlerFactory(Configuration conf, String user) {
        this.conf = new Configuration(conf);
        // force the resolution of the configuration files
        this.conf.getClass("fs.file.impl", null);
        this.handler = new HdfsUrlStreamHandler(this.conf, user);
    }

    public java.net.URLStreamHandler createURLStreamHandler(String protocol) {
        if (!protocols.containsKey(protocol)) {
            boolean known =
                    (conf.getClass("fs." + protocol + ".impl", null) != null);
            protocols.put(protocol, known);
        }
        if (protocols.get(protocol)) {
            return handler;
        } else {
            // FileSystem does not know the protocol, let the VM handle this
            return null;
        }
    }

}
