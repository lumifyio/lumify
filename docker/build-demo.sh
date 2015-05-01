#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
SRC_DIR=${DIR}/..
KEY_DIR=${SRC_DIR}/docker/demo/keys

rm -rf $KEY_DIR
mkdir -p $KEY_DIR
ssh-keygen -q -N "" -t dsa -f ${KEY_DIR}/ssh_host_dsa_key
ssh-keygen -q -N "" -t rsa -f ${KEY_DIR}/ssh_host_rsa_key
ssh-keygen -q -N "" -t rsa -f ${KEY_DIR}/id_rsa

cd $SRC_DIR
mvn -P "grunt unix",web-war,web-war-with-gpw,web-war-with-ui-plugins clean package -DskipTests

cd $DIR
mkdir -p demo/.tmp
cp ${SRC_DIR}/web/war/target/lumify-web-war-*.war demo/.tmp/root.war
cp ${SRC_DIR}/config/log4j.xml demo/.tmp
cp -R ${SRC_DIR}/examples/ontology-minimal demo/.tmp/ontology-minimal
# cp ${SRC_DIR}/web/plugins/auth-username-only/target/lumify-web-auth-username-only-*.jar demo/.tmp

case $(uname) in
  Linux)
    (cd ${DIR} && sudo docker build --file Dockerfile.demo --tag lumifyio/demo .)
    ;;
  Darwin)
    (cd ${DIR} && docker build --file Dockerfile.demo --tag lumifyio/demo .)
    ;;
  *)
    echo "unexpected uname: $(uname)"
    exit -1
    ;;
esac
