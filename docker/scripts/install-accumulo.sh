#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/accumulo-1.6.1-bin.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/accumulo-1.6.1-bin.tar.gz https://bits.lumify.io/extra/accumulo-1.6.1-bin.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/accumulo-1.6.1-bin.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
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
sed -i -e "s|<value>secret</value>|<value>password</value>|" /opt/accumulo/conf/accumulo-site.xml