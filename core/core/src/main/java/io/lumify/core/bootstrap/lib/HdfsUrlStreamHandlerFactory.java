package io.lumify.core.bootstrap.lib;

import org.apache.hadoop.conf.Configuration;

import java.net.URLStreamHandlerFactory;

public class HdfsUrlStreamHandlerFactory implements URLStreamHandlerFactory {

    private java.net.URLStreamHandler handler;

    public HdfsUrlStreamHandlerFactory(Configuration conf, String user) {
        this.handler = new HdfsUrlStreamHandler(conf, user);
    }

    public java.net.URLStreamHandler createURLStreamHandler(String protocol) {
        if (protocol.toLowerCase().equals("hdfs")) {
            return handler;
        }
        return null;
    }

}
