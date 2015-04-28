#!/bin/bash -eu

leptonica_version=1.71
leptonica_tgz=leptonica-${leptonica_version}.tar.gz

yum install libpng-devel
yum install libtiff-devel


wget --progress=dot -e dotbytes=1M -O /opt/${leptonica_tgz} http://www.leptonica.org/source/${leptonica_tgz}
tar -xzf /opt/${leptonica_tgz} -C /opt
rm /opt/${leptonica_tgz}

cd /opt/leptonica-${leptonica_version}
./configure
make
make install

cd /opt/lumify
rm -r /opt/leptonica-${leptonica_version}
