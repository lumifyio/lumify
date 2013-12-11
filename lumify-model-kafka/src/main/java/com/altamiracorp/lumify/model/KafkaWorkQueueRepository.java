package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Properties;

public class KafkaWorkQueueRepository extends WorkQueueRepository {
    private Producer<String, JSONObject> kafkaProducer;

    @Override
    public void init(Map config) {
        super.init(config);

        String zkServerNames = Configuration.getZkConnectString(config.get(Configuration.ZK_SERVERS), "/kafka");
        Properties props = new Properties();
        props.put("zk.connect", zkServerNames);
        props.put("serializer.class", KafkaJsonEncoder.class.getName());
        ProducerConfig producerConfig = new ProducerConfig(props);
        kafkaProducer = new Producer<String, JSONObject>(producerConfig);
    }

    @Override
    protected void pushOnQueue(String queueName, JSONObject json, String... extra) {
        if (extra != null && extra.length > 0) {
            JSONArray extraArray = new JSONArray();
            for (String e : extra) {
                extraArray.put(e);
            }
            json.put(KafkaJsonEncoder.EXTRA, extraArray);
        }

        json.put("sourceBolt", getClass().getName());

        ProducerData<String, JSONObject> data = new ProducerData<String, JSONObject>(queueName, json);
        kafkaProducer.send(data);
    }
}
