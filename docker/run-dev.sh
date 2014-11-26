#!/bin/bash

FS_DIR=$(pwd)/fs

mkdir -p ${FS_DIR}/var/log/hadoop
mkdir -p ${FS_DIR}/var/log/accumulo
mkdir -p ${FS_DIR}/var/log/elasticsearch
mkdir -p ${FS_DIR}/tmp/zookeeper
mkdir -p ${FS_DIR}/var/lib/hadoop-hdfs
mkdir -p ${FS_DIR}/var/local/hadoop
mkdir -p ${FS_DIR}/opt/elasticsearch-1.4.0/data
mkdir -p ${FS_DIR}/opt/rabbitmq_server-3.4.1/var

sudo docker run \
  -v ${FS_DIR}/../../:/opt/lumify \
  -v ${FS_DIR}/var/log:/var/log \
  -v ${FS_DIR}/tmp:/tmp \
  -v ${FS_DIR}/var/lib/hadoop-hdfs:/var/lib/hadoop-hdfs \
  -v ${FS_DIR}/var/local/hadoop:/var/local/hadoop \
  -v ${FS_DIR}/opt/elasticsearch-1.4.0/data:/opt/elasticsearch-1.4.0/data \
  -v ${FS_DIR}/opt/rabbitmq_server-3.4.1/var:/opt/rabbitmq_server-3.4.1/var \
  -p 2181:2181 `# ZooKeeper` \
  -p 5672:5672 `# RabbitMQ` \
  -p 5673:5673 `# RabbitMQ` \
  -p 8032:8032 `# Hadoop: Resource Manager` \
  -p 8088:8088 `# Hadoop: Resource Manager: Web UI` \
  -p 9000:9000 `# Hadoop: Name Node: Metadata Service` \
  -p 9200:9200 `# Elasticsearch` \
  -p 9300:9300 `# Elasticsearch` \
  -p 9997:9997 `# Accumulo` \
  -p 9999:9999 `# Accumulo` \
  -p 15672:15672 `# RabbitMQ: Web UI` \
  -p 50010:50010 `# Hadoop: Data Node: Data transfer` \
  -p 50020:50020 `# Hadoop: Data Node: Metadata operations` \
  -p 50030:50030 `# Hadoop: Job Tracker` \
  -p 50060:50060 `# Hadoop: Node Manager: Web UI` \
  -p 50070:50070 `# Hadoop: Name Node: Web UI` \
  -p 50075:50075 `# Hadoop: Data Node: Web UI` \
  -p 50090:50090 `# Hadoop: Secondary Name Node` \
  -p 50095:50095 `# Accumulo: Web UI` \
  -i \
  -t \
  -h lumify-dev \
  lumifyio/dev \
  $1
