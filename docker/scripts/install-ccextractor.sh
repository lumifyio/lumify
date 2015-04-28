#!/bin/bash

ccextractor_version=0.76
ccextractor_zip=ccextractor.src.${ccextractor_version}.zip

wget --progress=dot -e dotbytes=1M -O /opt/${ccextractor_zip} http://sourceforge.net/projects/ccextractor/files/ccextractor/${ccextractor_version}/${ccextractor_zip}/download

unzip /opt/${ccextractor_zip} -d /opt
rm /opt/${ccextractor_zip}

cd /opt/ccextractor.0.76/linux
gcc -std=gnu99 -Wno-write-strings -DGPAC_CONFIG_LINUX -D_FILE_OFFSET_BITS=64  -I../src/lib_ccx/ -I../src/gpacmp4/ -I../src/libpng/ -I../src/zlib/ -o ccextractor $(find ../src/ -name '*.cpp') $(find ../src/ -name '*.c') -lm -Xlinker -zmuldefs

cp /opt/ccextractor.0.76/linux/ccextractor /usr/local/bin/ccextractor