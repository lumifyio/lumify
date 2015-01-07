package io.lumify.web.clientapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lumify.web.clientapi.model.util.ObjectMapperFactory;

public class JsonUtil {
    public static ObjectMapper mapper = ObjectMapperFactory.getInstance();

    public static ObjectMapper getJsonMapper() {
        return mapper;
    }
}

