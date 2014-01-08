package com.altamiracorp.lumify.model;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Singleton;
import kafka.javaapi.producer.Producer;
import kafka.producer.KeyedMessage;
import kafka.producer.ProducerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Properties;

@Singleton
public class KafkaWorkQueueRepository extends WorkQueueRepository {
    public static final String KAFKA_PATH_PREFIX = "/kafka";
    private static final Object kafkaProducerLock = new Object();
    private static Producer<String, JSONObject> kafkaProducer;
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KafkaWorkQueueRepository.class);

    @Override
    public void init(Map config) {
        super.init(config);

        synchronized (kafkaProducerLock) {
            if (kafkaProducer == null) {
                String zkServerNames = fixZkServerNames("" + config.get(Configuration.ZK_SERVERS));
                LOGGER.info("Kafka Work Queue Repository zkServerNames: %s", zkServerNames);
                Properties props = new Properties();
                props.put("metadata.broker.list", zkServerNames);
                props.put("serializer.class", KafkaJsonEncoder.class.getName());
                ProducerConfig producerConfig = new ProducerConfig(props);
                try {
                    kafkaProducer = new Producer<String, JSONObject>(producerConfig);
                } catch (Exception ex) {
                    throw new RuntimeException("Failed to create kafka producer. Have you run /opt/lumify/kafka-clear.sh?", ex);
                }
            }
        }
    }

    private String fixZkServerNames(String zkServerNames) {
        String[] zkServerNamesArray = zkServerNames.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < zkServerNamesArray.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(zkServerNamesArray[i]);
            if (zkServerNamesArray[i].indexOf(':') < 0) {
                sb.append(":9092");
            }
        }
        return sb.toString();
    }

    @Override
    public void pushOnQueue(String queueName, JSONObject json, String... extra) {
        if (extra != null && extra.length > 0) {
            JSONArray extraArray = new JSONArray();
            for (String e : extra) {
                extraArray.put(e);
            }
            json.put(KafkaJsonEncoder.EXTRA, extraArray);
        }

        json.put("sourceBolt", getClass().getName());

        KeyedMessage<String, JSONObject> data = new KeyedMessage<String, JSONObject>(queueName, json);
        kafkaProducer.send(data);
    }
}
