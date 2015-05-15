#!/bin/bash

ccextractor_version=0.76
ccextractor_zip=ccextractor.src.${ccextractor_version}.zip

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/${ccextractor_zip}" ]; then
    curl -L -o $ARCHIVE_DIR/${ccextractor_zip} https://bits.lumify.io/extra/${ccextractor_zip}
fi

# extract from the archive
unzip $ARCHIVE_DIR/${ccextractor_zip} -d /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
cd /opt/ccextractor.0.76/linux
gcc -std=gnu99 -Wno-write-strings -DGPAC_CONFIG_LINUX -D_FILE_OFFSET_BITS=64  -I../src/lib_ccx/ -I../src/gpacmp4/ -I../src/libpng/ -I../src/zlib/ -o ccextractor $(find ../src/ -name '*.cpp') $(find ../src/ -name '*.c') -lm -Xlinker -zmuldefs
cp /opt/ccextractor.0.76/linux/ccextractor /usr/local/bin/ccextractor