#!/bin/bash -eu

jetty_version=9.2.7.v20150116
jetty_tgz=jetty-distribution-${jetty_version}.tar.gz

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/$jetty_tgz" ]; then
    curl -L -o $ARCHIVE_DIR/${jetty_tgz} https://bits.lumify.io/extra/${jetty_tgz}
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/${jetty_tgz} -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/jetty-distribution-${jetty_version} /opt/jetty
ln -s /opt/jetty/bin/jetty.sh /etc/init.d/jetty
