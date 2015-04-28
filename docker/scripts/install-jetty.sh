#!/bin/bash -eu

jetty_version=9.2.7.v20150116
jetty_tgz=jetty-distribution-${jetty_version}.tar.gz

curl -L -o /opt/${jetty_tgz} https://bits.lumify.io/extra//${jetty_tgz}
tar -xzf /opt/${jetty_tgz} -C /opt
rm /opt/${jetty_tgz}
ln -s /opt/jetty-distribution-${jetty_version} /opt/jetty
ln -s /opt/jetty/bin/jetty.sh /etc/init.d/jetty
