package com.altamiracorp.lumify.model;

import backtype.storm.spout.MultiScheme;
import backtype.storm.tuple.Fields;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import kafka.serializer.Encoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KafkaJsonEncoder implements Encoder<JSONObject>, MultiScheme {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KafkaJsonEncoder.class);
    public static final String EXTRA = "_extra";

    @Override
    public byte[] toBytes(JSONObject json) {
        return json.toString().getBytes();
    }

    @Override
    public Iterable<List<Object>> deserialize(byte[] ser) {
        String serString = new String(ser);
        try {
            ArrayList<Object> results = new ArrayList<Object>();
            JSONObject json = new JSONObject(serString);
            results.add(json.toString());

            JSONArray extra = json.optJSONArray(EXTRA);
            if (extra != null) {
                for (int i = 0; i < extra.length(); i++) {
                    results.add(extra.get(i));
                }
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("deserialize: %s (size: %d)", json.toString(), results.size());
            }
            List<List<Object>> it = new ArrayList<List<Object>>();
            it.add(results);
            return it;
        } catch (Exception ex) {
            String head = serString;
            if (head.length() > 20) {
                head = head.substring(0, 20);
            }
            throw new RuntimeException("Could not deserialize [" + head + "]", ex);
        }
    }

    @Override
    public Fields getOutputFields() {
        return new Fields("json");
    }
}
