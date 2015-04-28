#!/bin/bash -eu

curl -L -o /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz https://bits.lumify.io/extra/rabbitmq-server-generic-unix-3.4.1.tar.gz
tar -xzf /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz -C /opt
rm /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz
ln -s /opt/rabbitmq_server-3.4.1 /opt/rabbitmq
