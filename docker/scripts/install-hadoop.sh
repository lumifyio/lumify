#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/hadoop-2.3.0.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/hadoop-2.3.0.tar.gz https://bits.lumify.io/extra/hadoop-2.3.0.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/hadoop-2.3.0.tar.gz -C /opt/

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/hadoop-2.3.0 /opt/hadoop
sed -i '/^export JAVA_HOME/ s:.*:export JAVA_HOME=/opt/jdk\nexport HADOOP_PREFIX=/opt/hadoop\nexport HADOOP_HOME=/opt/hadoop\n:' /opt/hadoop/etc/hadoop/hadoop-env.sh
sed -i '/^export HADOOP_CONF_DIR/ s:.*:export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop/:' /opt/hadoop/etc/hadoop/hadoop-env.sh
sed -i '/^hadoop.log.dir=.*/ s:.*:hadoop.log.dir=/var/log/hadoop:' /opt/hadoop/etc/hadoop/log4j.properties
sed -i 's|YARN_OPTS="$YARN_OPTS -Dhadoop.log.dir=$YARN_LOG_DIR"|YARN_OPTS="$YARN_OPTS -Djava.net.preferIPv4Stack=true"\nYARN_OPTS="$YARN_OPTS -Dhadoop.log.dir=$YARN_LOG_DIR"|' /opt/hadoop/etc/hadoop/yarn-env.sh
mkdir -p /opt/hadoop/input
mkdir -p /var/log/hadoop
mkdir -p /var/log/hadoop-yarn/apps
rm -rf /opt/hadoop-2.3.0/logs
ln -s /var/log/hadoop /opt/hadoop-2.3.0/logs
chmod +x /opt/hadoop/etc/hadoop/*-env.sh
rm -rf /opt/hadoop/lib/native/*
tar -xzf /opt/hadoop/hadoop-native-64bit.tar.gz -C /opt/hadoop/lib/native/

cp /opt/hadoop/etc/hadoop/*.xml /opt/hadoop/input
sed s/HOSTNAME/localhost/ /opt/hadoop/etc/hadoop/core-site.xml.template > /opt/hadoop/etc/hadoop/core-site.xml
