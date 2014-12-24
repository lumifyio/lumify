#!/bin/bash -eu

wget --header "Cookie: oraclelicense=accept-securebackup-cookie" -O /opt/jdk-7u71-linux-x64.tar.gz http://download.oracle.com/otn-pub/java/jdk/7u71-b14/jdk-7u71-linux-x64.tar.gz
tar -xzf /opt/jdk-7u71-linux-x64.tar.gz -C /opt
rm /opt/jdk-7u71-linux-x64.tar.gz
ln -s /opt/jdk1.7.0_71 /opt/jdk
