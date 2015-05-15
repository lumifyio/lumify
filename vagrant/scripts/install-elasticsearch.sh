#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/elasticsearch-1.4.4.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/elasticsearch-1.4.4.tar.gz https://bits.lumify.io/extra/elasticsearch-1.4.4.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/elasticsearch-1.4.4.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/elasticsearch-1.4.4 /opt/elasticsearch
rm -rf /opt/elasticsearch-1.4.4/logs
mkdir -p /var/log/elasticsearch
ln -s /var/log/elasticsearch /opt/elasticsearch-1.4.4/logs
/opt/elasticsearch/bin/plugin -install mobz/elasticsearch-head
/opt/elasticsearch/bin/plugin -install org.securegraph/securegraph-elasticsearch-plugin/0.8.0
