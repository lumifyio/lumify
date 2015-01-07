#!/bin/bash -eu

dist=jetty-distribution-9.2.6.v20141205
pkg=${dist}.tar.gz

wget -O /opt/$pkg http://download.eclipse.org/jetty/stable-9/dist/${pkg}
tar -xzf /opt/$pkg -C /opt
rm /opt/$pkg
ln -s /opt/$dist /opt/jetty
ln -s /opt/jetty/bin/jetty.sh /etc/init.d/jetty
