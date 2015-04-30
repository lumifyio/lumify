#!/bin/bash -eu

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/apache-maven-3.2.5-bin.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/apache-maven-3.2.5-bin.tar.gz https://bits.lumify.io/extra/apache-maven-3.2.5-bin.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/apache-maven-3.2.5-bin.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/apache-maven-3.2.5 /opt/maven
