#!/bin/bash -eu

wget -O /opt/elasticsearch-1.4.1.tar.gz https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.1.tar.gz
tar -xzf /opt/elasticsearch-1.4.1.tar.gz -C /opt
rm /opt/elasticsearch-1.4.1.tar.gz
ln -s /opt/elasticsearch-1.4.1 /opt/elasticsearch
rm -rf /opt/elasticsearch-1.4.1/logs
mkdir -p /var/log/elasticsearch
ln -s /var/log/elasticsearch /opt/elasticsearch-1.4.1/logs
/opt/elasticsearch/bin/plugin -install mobz/elasticsearch-head
/opt/elasticsearch/bin/plugin -install org.securegraph/securegraph-elasticsearch-plugin/0.8.0
