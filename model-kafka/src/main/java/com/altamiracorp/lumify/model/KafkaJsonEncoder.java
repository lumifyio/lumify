package com.altamiracorp.lumify.model;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import kafka.message.Message;
import kafka.serializer.Encoder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class KafkaJsonEncoder implements Encoder<JSONObject>, Scheme {
    private static final Logger LOGGER = LoggerFactory.getLogger(KafkaJsonEncoder.class);
    public static final String EXTRA = "_extra";

    @Override
    public Message toMessage(JSONObject json) {
        return new Message(json.toString().getBytes());
    }

    @Override
    public List<Object> deserialize(byte[] ser) {
        ArrayList<Object> results = new ArrayList<Object>();
        JSONObject json = new JSONObject(new String(ser));
        results.add(json.toString());

        JSONArray extra = json.optJSONArray(EXTRA);
        if (extra != null) {
            for (int i = 0; i < extra.length(); i++) {
                results.add(extra.get(i));
            }
        }

        LOGGER.info("deserialize: " + json.toString() + " (size: " + results.size() + ")");
        return results;
    }

    @Override
    public Fields getOutputFields() {
        return new Fields("json");
    }
}
