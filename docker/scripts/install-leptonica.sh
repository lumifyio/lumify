#!/bin/bash -eu

leptonica_version=1.71
leptonica_tgz=leptonica-${leptonica_version}.tar.gz

yum install libpng-devel
yum install libtiff-devel


curl -L -o /opt/${leptonica_tgz} https://bits.lumify.io/extra/${leptonica_tgz}
tar -xzf /opt/${leptonica_tgz} -C /opt
rm /opt/${leptonica_tgz}

cd /opt/leptonica-${leptonica_version}
./configure
make
make install

cd /opt/lumify
rm -r /opt/leptonica-${leptonica_version}
