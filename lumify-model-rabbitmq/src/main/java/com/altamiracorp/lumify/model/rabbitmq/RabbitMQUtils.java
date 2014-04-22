package com.altamiracorp.lumify.model.rabbitmq;

import com.altamiracorp.lumify.core.config.Configuration;
import com.altamiracorp.lumify.core.exception.LumifyException;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RabbitMQUtils {
    private static final int DEFAULT_PORT = 5672;

    public static Channel openChannel(Configuration configuration) {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            Address[] addresses = getAddresses(configuration);
            if (addresses.length == 0) {
                throw new LumifyException("Could not configure RabbitMQ. No addresses specified. expecting configuration parameter 'rabbitmq.addr.0.host'.");
            }
            Connection connection = factory.newConnection(addresses);
            return connection.createChannel();
        } catch (IOException ex) {
            throw new LumifyException("Could not open channel to RabbitMQ", ex);
        }
    }

    private static Address[] getAddresses(Configuration configuration) {
        List<Address> addresses = new ArrayList<Address>();
        for (int i = 0; i < 1000; i++) {
            String host = configuration.get("rabbitmq.addr." + i + ".host");
            if (host != null) {
                int port = configuration.getInt("rabbitmq.addr." + i + ".port", DEFAULT_PORT);
                addresses.add(new Address(host, port));
            }
        }

        return addresses.toArray(new Address[addresses.size()]);
    }
}
