#!/bin/bash -eu

jetty_version=9.2.6.v20141205
jetty_tgz=jetty-distribution-${jetty_version}.tar.gz

wget -O /opt/${jetty_tgz} http://download.eclipse.org/jetty/${jetty_version}/dist/${jetty_tgz}
tar -xzf /opt/${jetty_tgz} -C /opt
rm /opt/${jetty_tgz}
ln -s /opt/jetty-distribution-${jetty_version} /opt/jetty
ln -s /opt/jetty/bin/jetty.sh /etc/init.d/jetty
