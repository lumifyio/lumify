package com.altamiracorp.lumify.contentTypeExtraction;

import org.apache.hadoop.mapreduce.Mapper;

import java.io.InputStream;

public interface ContentTypeExtractor {
    void setup(Mapper.Context context);

    public String extract(InputStream in, String fileExt) throws Exception;
}
