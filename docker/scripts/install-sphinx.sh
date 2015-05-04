#!/bin/bash -eu

sphinxbase_version=0.8
sphinxbase_tgz=sphinxbase-${sphinxbase_version}.tar.gz

pocketsphinx_version=0.8
pocketsphinx_tgz=pocketsphinx-${sphinxbase_version}.tar.gz

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archives
if [ ! -f "$ARCHIVE_DIR/${sphinxbase_tgz}" ]; then
    curl -L -o $ARCHIVE_DIR/${sphinxbase_tgz} https://bits.lumify.io/extra/${sphinxbase_tgz}
fi
if [ ! -f "$ARCHIVE_DIR/${pocketsphinx_tgz}" ]; then
    curl -L -o $ARCHIVE_DIR/${pocketsphinx_tgz} https://bits.lumify.io/extra/${pocketsphinx_tgz}
fi

# extract from the archives
tar -xvf $ARCHIVE_DIR/${sphinxbase_tgz} -C /tmp
tar -xvf $ARCHIVE_DIR/${pocketsphinx_tgz} -C /tmp

# delete the archive
rm -rf $ARCHIVE_DIR

# install sphinxbase
cd /tmp/sphinxbase-${sphinxbase_version}
./configure
make
make install

# install pocketshpinx
cd /tmp/pocketsphinx-${pocketsphinx_version}
./configure
make
make install


rm -rf /tmp/sphinxbase-${sphinxbase_version}
rm -rf /tmp/pocketsphinx-${pocketsphinx_version}




