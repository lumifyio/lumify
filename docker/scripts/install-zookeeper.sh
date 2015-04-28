#!/bin/bash -eu

curl -L -o /opt/zookeeper-3.4.6.tar.gz https://bits.lumify.io/extra/zookeeper-3.4.6.tar.gz
tar -xzf /opt/zookeeper-3.4.6.tar.gz -C /opt
rm /opt/zookeeper-3.4.6.tar.gz
ln -s /opt/zookeeper-3.4.6 /opt/zookeeper
cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg
mkdir -p /tmp/zookeeper
