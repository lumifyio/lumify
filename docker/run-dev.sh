#!/bin/bash

FS_DIR=$(pwd)/fs

mkdir -p ${FS_DIR}/var/log
mkdir -p ${FS_DIR}/tmp/zookeeper
mkdir -p ${FS_DIR}/var/lib/hadoop-hdfs
mkdir -p ${FS_DIR}/var/local/hadoop
mkdir -p ${FS_DIR}/opt/hadoop-2.5.1/logs
mkdir -p ${FS_DIR}/opt/accumulo-1.6.1/logs
mkdir -p ${FS_DIR}/opt/elasticsearch-1.4.0/logs

sudo docker run \
  -v ${FS_DIR}/../../:/opt/lumify \
  -v ${FS_DIR}/var/log:/var/log \
  -v ${FS_DIR}/tmp:/tmp \
  -v ${FS_DIR}/var/lib/hadoop-hdfs:/var/lib/hadoop-hdfs \
  -v ${FS_DIR}/var/local/hadoop:/var/local/hadoop \
  -v ${FS_DIR}/opt/hadoop/logs:/opt/hadoop-2.5.1/logs \
  -v ${FS_DIR}/opt/accumulo-1.6.1/logs:/opt/accumulo-1.6.1/logs \
  -v ${FS_DIR}/opt/elasticsearch-1.4.0/logs:/opt/elasticsearch-1.4.0/logs \
  -p 2181:2181 \
  -p 9200:9200 \
  -p 9300:9300 \
  -p 9997:9997 \
  -p 9999:9999 \
  -p 50010:50010 \
  -p 50020:50020 \
  -p 50030:50030 \
  -p 50070:50070 \
  -p 50075:50075 \
  -p 50090:50090 \
  -p 50095:50095 \
  -i \
  -t \
  lumifyio/dev \
  $1
