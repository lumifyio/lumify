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

        String zkServerNames = getZkServerNames(config);
        Properties props = new Properties();
        props.put("zk.connect", zkServerNames);
        props.put("serializer.class", KafkaJsonEncoder.class.getName());
        ProducerConfig producerConfig = new ProducerConfig(props);
        kafkaProducer = new Producer<String, JSONObject>(producerConfig);
    }

    private String getZkServerNames(Map config) {
        String zkServersString = (String) config.get(Configuration.ZK_SERVERS);
        String[] zkServersArray = zkServersString.split(",");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < zkServersArray.length; i++) {
            if (i > 0) {
                result.append(",");
            }
            result.append(zkServersArray[i]);
            result.append("/kafka");

            // TODO when kafka is upgraded to 0.8.x try this again with multiple zk servers.
            //      Currently kafka only handles one.
            //      (see http://mail-archives.apache.org/mod_mbox/kafka-users/201305.mbox/%3CCAA+BczQKa_JS=i--U-3v8-Lq4udoRc6xmoUcMdrMYOBe-NckZg@mail.gmail.com%3E)
            break;
        }
        return result.toString();
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
