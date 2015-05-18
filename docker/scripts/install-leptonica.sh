#!/bin/bash -eu

leptonica_version=1.71
leptonica_tgz=leptonica-${leptonica_version}.tar.gz

yum install libpng-devel
yum install libtiff-devel

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/${leptonica_tgz}" ]; then
    curl -L -o $ARCHIVE_DIR/${leptonica_tgz} https://bits.lumify.io/extra/${leptonica_tgz}
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/${leptonica_tgz} -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
cd /opt/leptonica-${leptonica_version}
./configure
make
make install

# delete the src dir
cd /opt/lumify
rm -r /opt/leptonica-${leptonica_version}
