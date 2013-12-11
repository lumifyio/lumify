package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

public class KafkaWorkQueueRepository extends WorkQueueRepository {
    private Producer<String, JSONObject> kafkaProducer;

    @Override
    public void init(Map config) {
        super.init(config);

        String zkServerNames = getZkConnectString(config.get(Configuration.ZK_SERVERS));
        Properties props = new Properties();
        props.put("zk.connect", zkServerNames);
        props.put("serializer.class", KafkaJsonEncoder.class.getName());
        ProducerConfig producerConfig = new ProducerConfig(props);
        kafkaProducer = new Producer<String, JSONObject>(producerConfig);
    }

    private String getZkConnectString(Object zkServers) {
        String zkServerNames;
        if (zkServers instanceof Collection) {
            Collection c = (Collection) zkServers;
            StringBuilder temp = new StringBuilder();
            boolean first = true;
            for (Object zkServer : c) {
                if (!first) {
                    temp.append(",");
                }
                temp.append(zkServer.toString());
                temp.append("/kafka");
                first = false;
            }
            zkServerNames = temp.toString();
        } else {
            zkServerNames = zkServers + "/kafka";
        }
        return zkServerNames;
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
