#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/rabbitmq-server-generic-unix-3.4.1.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/rabbitmq-server-generic-unix-3.4.1.tar.gz https://bits.lumify.io/extra/rabbitmq-server-generic-unix-3.4.1.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/rabbitmq-server-generic-unix-3.4.1.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/rabbitmq_server-3.4.1 /opt/rabbitmq
