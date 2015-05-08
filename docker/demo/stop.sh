#!/bin/bash

function stop_msg {
  echo -e "\n\e[32mStopping $1\n---------------------------------------------------------------\e[0m"
}

stop_msg "Jetty"
/opt/jetty/bin/jetty.sh stop

stop_msg "RabbitMQ"
/opt/rabbitmq/sbin/rabbitmqctl stop

stop_msg "Elasticsearch"
curl -XPOST 'http://localhost:9200/_cluster/nodes/_local/_shutdown'
echo ""

stop_msg "Accumulo"
/opt/accumulo/bin/stop-all.sh

stop_msg "Hadoop"
/opt/hadoop/sbin/stop-yarn.sh
/opt/hadoop/sbin/stop-dfs.sh

stop_msg "ZooKeeper"
/opt/zookeeper/bin/zkServer.sh stop

stop_msg "SSHD"
service sshd stop

echo ""
