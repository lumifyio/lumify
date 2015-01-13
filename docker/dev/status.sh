#!/bin/bash

function _status_begin {
    echo -en "$(echo $1 | awk '{ printf "%-20s", $1}')[TEST]"
}

function _status_good {
    echo -e "\r$(echo $1 | awk '{ printf "%-20s", $1}')[\e[32m OK\e[0m ]"
}

function _status_fail {
    echo -e "\r$(echo $1 | awk '{ printf "%-20s", $1}')[\e[31mFAIL\e[0m]"
}

function status_zookeeper {
    _status_begin "Zookeeper"
    if [[ $(/opt/zookeeper/bin/zkServer.sh status 2>&1) =~ "Mode: standalone" ]]; then
        _status_good "Zookeeper"
    else
        _status_fail "Zookeeper"
    fi
}

function status_hadoop {
    _status_begin "Hadoop"
    if [[ $(/opt/hadoop/bin/hdfs dfsadmin -report 2>&1) =~ "Datanodes available: 1" ]]; then
        _status_good "Hadoop"
    else
        _status_fail "Hadoop"
    fi
}

function status_accumulo {
    _status_begin "Accumulo"
    if [[ $(/opt/accumulo/bin/accumulo admin ping 2>&1) =~ "0 of 1 tablet servers unreachable" ]]; then
        _status_good "Accumulo"
    else
        _status_fail "Accumulo"
    fi
}

function status_elasticsearch {
    _status_begin "Elasticsearch"
    if [[ $(curl -XGET 'http://localhost:9200/_status' 2>&1) =~ "\"successful\":1" ]]; then
        _status_good "Elasticsearch"
    else
        _status_fail "Elasticsearch"
    fi
}

function status_rabbitmq {
    _status_begin "RabbitMQ"
    if [[ $(/opt/rabbitmq/sbin/rabbitmqctl status 2>&1) =~ "pid," ]]; then
        _status_good "RabbitMQ"
    else
        _status_fail "RabbitMQ"
    fi
}

echo "Checking Status..."
status_zookeeper
status_hadoop
status_accumulo
status_elasticsearch
status_rabbitmq
