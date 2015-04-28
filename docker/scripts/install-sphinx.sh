#!/bin/bash -eu

sphinxbase_version=0.8
sphinxbase_tgz=sphinxbase-${sphinxbase_version}.tar.gz

pocketsphinx_version=0.8
pocketsphinx_tgz=pocketsphinx-${sphinxbase_version}.tar.gz

cd /tmp
curl -L -o ${sphinxbase_tgz} -O ls -l${sphinxbase_version}/${sphinxbase_tgz}?use_mirror=hivelocity
tar -xvf /tmp/${sphinxbase_tgz} -C /tmp
cd /tmp/sphinxbase-${sphinxbase_version}
./configure
make
make install


# install pocketshpinx
cd /tmp
curl -L -o ${pocketsphinx_tgz} -O http://downloads.sourceforge.net/project/cmusphinx/pocketsphinx/${pocketsphinx_version}/${pocketsphinx_tgz}?use_mirror=hivelocity
tar -xvf /tmp/${pocketsphinx_tgz} -C /tmp
cd /tmp/pocketsphinx-${pocketsphinx_version}
./configure
make
make install




