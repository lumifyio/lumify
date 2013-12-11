package com.altamiracorp.lumify.core.contentTypeExtraction;

import org.apache.hadoop.mapreduce.Mapper;

import java.io.InputStream;
import java.util.Map;

public interface ContentTypeExtractor {
    public String extract(InputStream in, String fileExt) throws Exception;

    void init(Map map);
}
