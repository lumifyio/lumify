#!/bin/bash -eu

wget -O /opt/apache-maven-3.2.3-bin.tar.gz http://www.us.apache.org/dist/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz
tar -xzf /opt/apache-maven-3.2.3-bin.tar.gz -C /opt
rm /opt/apache-maven-3.2.3-bin.tar.gz
ln -s /opt/apache-maven-3.2.3 /opt/maven
