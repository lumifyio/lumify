package com.altamiracorp.lumify.model;

import backtype.storm.topology.IRichSpout;
import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.model.workQueue.WorkQueueRepository;
import com.altamiracorp.lumify.core.util.LumifyLogger;
import com.altamiracorp.lumify.core.util.LumifyLoggerFactory;
import com.google.inject.Singleton;
import kafka.javaapi.producer.Producer;
import kafka.javaapi.producer.ProducerData;
import kafka.producer.ProducerConfig;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Map;
import java.util.Properties;

@Singleton
public class KafkaWorkQueueRepository extends WorkQueueRepository {
    private static final LumifyLogger LOGGER = LumifyLoggerFactory.getLogger(KafkaWorkQueueRepository.class);
    public static final String KAFKA_PATH_PREFIX = "/kafka";
    private static final Object kafkaProducerLock = new Object();
    private static Producer<String, JSONObject> kafkaProducer;

    @Override
    public void init(Map config) {
        super.init(config);

        synchronized (kafkaProducerLock) {
            if (kafkaProducer == null) {
                String zkServerNames = config.get(Configuration.ZK_SERVERS) + KAFKA_PATH_PREFIX;
                LOGGER.info("Kafka Work Queue Repository zkServerNames: %s", zkServerNames);
                Properties props = new Properties();
                props.put("zk.connect", zkServerNames);
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

    @Override
    public IRichSpout createSpout(Configuration configuration, String queueName, Long queueStartOffsetTime) {
        return new LumifyKafkaSpout(configuration, queueName, queueStartOffsetTime);
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

        ProducerData<String, JSONObject> data = new ProducerData<String, JSONObject>(queueName, json);
        kafkaProducer.send(data);
    }
}
