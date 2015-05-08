#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/zookeeper-3.4.6.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/zookeeper-3.4.6.tar.gz https://bits.lumify.io/extra/zookeeper-3.4.6.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/zookeeper-3.4.6.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/zookeeper-3.4.6 /opt/zookeeper
cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg
mkdir -p /tmp/zookeeper
