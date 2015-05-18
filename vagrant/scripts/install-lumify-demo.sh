#!/bin/bash -eu

# Create Lumify install dir
echo "Create Lumify install dir"
mkdir -p /opt/lumify
chown root:root /opt/lumify
chmod 600 /opt/lumify

# Install CLAVIN index
echo "Install CLAVIN index"
/bin/bash /vagrant/vagrant/scripts/install-clavin.sh

# Install Lumify
echo "Install Lumify Web App"
cp /vagrant/web/war/target/lumify-web-war-*.war /opt/jetty/webapps/root.war
mkdir -p /opt/lumify/config /opt/lumify/ontology /opt/lumify/lib /opt/lumify/logs
cp /vagrant/config/log4j.xml /opt/lumify/config/log4j.xml
cp /vagrant/vagrant/demo/lumify.properties /opt/lumify/config/lumify.properties
cp -R /vagrant/config/knownEntities /opt/lumify/config
cp -R /vagrant/config/opencv /opt/lumify/config
cp -R /vagrant/config/opennlp /opt/lumify/config
mkdir -p /opt/lumify/config/ontology
cp -R /vagrant/examples/ontology-minimal /opt/lumify/config/ontology

