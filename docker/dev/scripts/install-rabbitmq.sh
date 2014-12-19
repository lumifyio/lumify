#!/bin/bash -eu

wget -O /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz http://www.rabbitmq.com/releases/rabbitmq-server/v3.4.1/rabbitmq-server-generic-unix-3.4.1.tar.gz
tar -xzf /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz -C /opt
rm /opt/rabbitmq-server-generic-unix-3.4.1.tar.gz
ln -s /opt/rabbitmq_server-3.4.1 /opt/rabbitmq
