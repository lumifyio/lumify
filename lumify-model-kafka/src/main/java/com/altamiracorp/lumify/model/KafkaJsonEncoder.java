package com.altamiracorp.lumify.model;

import backtype.storm.spout.Scheme;
import backtype.storm.tuple.Fields;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import java.nio.charset.Charset;
import kafka.message.Message;
import kafka.serializer.Encoder;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class KafkaJsonEncoder implements Encoder<JSONObject>, Scheme {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KafkaJsonEncoder.class);
    public static final String EXTRA = "_extra";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    @Override
    public Message toMessage(JSONObject json) {
        return new Message(json.toString().getBytes(UTF8));
    }

    @Override
    public List<Object> deserialize(byte[] ser) {
        String serString = new String(ser, UTF8);
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
            return results;
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
