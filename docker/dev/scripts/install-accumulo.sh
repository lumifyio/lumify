#!/bin/bash -eu

wget -O /opt/accumulo-1.6.1-bin.tar.gz http://www.us.apache.org/dist/accumulo/1.6.1/accumulo-1.6.1-bin.tar.gz
tar -xzf /opt/accumulo-1.6.1-bin.tar.gz -C /opt
rm /opt/accumulo-1.6.1-bin.tar.gz
ln -s /opt/accumulo-1.6.1 /opt/accumulo
rm -rf /opt/accumulo-1.6.1/logs
mkdir -p /var/log/accumulo
ln -s /var/log/accumulo /opt/accumulo-1.6.1/logs
cp /opt/accumulo/conf/examples/1GB/standalone/* /opt/accumulo/conf
sed -i -e "s|HADOOP_PREFIX=/path/to/hadoop|HADOOP_PREFIX=/opt/hadoop|" \
    -e "s|JAVA_HOME=/path/to/java|JAVA_HOME=/opt/jdk|" \
    -e "s|ZOOKEEPER_HOME=/path/to/zookeeper|ZOOKEEPER_HOME=/opt/zookeeper|" \
    -e "s|.*export ACCUMULO_MONITOR_BIND_ALL.*|export ACCUMULO_MONITOR_BIND_ALL=\"true\"|" \
    /opt/accumulo/conf/accumulo-env.sh
sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/masters
sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/slaves
sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/tracers
sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/gc
sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/monitor
