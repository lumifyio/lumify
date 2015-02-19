package io.lumify.palantir.service;

import io.lumify.palantir.sqlrunner.SqlRunner;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

public interface Exporter {
    void run(ExporterSource exporterSource) throws Exception;

    public interface ExporterSource {
        Configuration getHadoopConfiguration();

        Path getDestinationPath();

        SqlRunner getSqlRunner();

        FileSystem getFileSystem();

        void writeFile(String fileName, String data);
    }
}
