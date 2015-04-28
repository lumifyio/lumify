#!/bin/bash -eu

curl -L -o /opt/apache-maven-3.2.5-bin.tar.gz https://bits.lumify.io/extra/apache-maven-3.2.5-bin.tar.gz
tar -xzf /opt/apache-maven-3.2.5-bin.tar.gz -C /opt
rm -f /opt/apache-maven-3.2.5-bin.tar.gz
ln -s /opt/apache-maven-3.2.5 /opt/maven
