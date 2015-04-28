#!/bin/bash -eu

wget -O /opt/apache-maven-3.2.5-bin.tar.gz http://www.us.apache.org/dist/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
tar -xzf /opt/apache-maven-3.2.5-bin.tar.gz -C /opt
rm -f /opt/apache-maven-3.2.5-bin.tar.gz
ln -s /opt/apache-maven-3.2.5 /opt/maven
