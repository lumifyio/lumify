#!/bin/sh

#
# This script will deploy Lumify to a running Docker container
#

DIR=$(cd $(dirname "$0") && pwd)
SRC_DIR=$DIR/..

SSH_KEY=$DIR/demo/keys/id_rsa
SSH_OPTIONS="-p 2022 -i $SSH_KEY -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"
SCP_OPTIONS="-P 2022 -i $SSH_KEY -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no"

# stop Jetty
ssh $SSH_OPTIONS root@lumify-demo '/opt/stop.sh'

# deploy Lumify artifacts
scp $SCP_OPTIONS    $SRC_DIR/config/log4j.xml                     root@lumify-demo:/opt/lumify/config/log4j.xml
scp $SCP_OPTIONS    $SRC_DIR/docker/demo/lumify.properties        root@lumify-demo:/opt/lumify/config/lumify.properties
scp $SCP_OPTIONS -r $SRC_DIR/config/knownEntities                 root@lumify-demo:/opt/lumify/config/knownEntities
scp $SCP_OPTIONS -r $SRC_DIR/config/opencv                        root@lumify-demo:/opt/lumify/config/opencv
scp $SCP_OPTIONS -r $SRC_DIR/config/opennlp                       root@lumify-demo:/opt/lumify/config/opennlp
scp $SCP_OPTIONS    $SRC_DIR/web/war/target/lumify-web-war-*.war  root@lumify-demo:/opt/jetty/webapps/root.war
scp $SCP_OPTIONS -r $SRC_DIR/examples/ontology-minimal            root@lumify-demo:/opt/lumify/ontology

# start Jetty
ssh $SSH_OPTIONS root@lumify-demo '/opt/start.sh </dev/null >/dev/null 2>&1 &'

exit 0
