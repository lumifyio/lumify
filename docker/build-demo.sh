#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
cd ${DIR}

SRC_DIR=${DIR}/..

cp ${SRC_DIR}/web/war/target/lumify-web-war-*.war demo/root.war
cp ${SRC_DIR}/web/plugins/auth-username-only/target/lumify-web-auth-username-only-*.jar demo
cp ${SRC_DIR}/config/log4j.xml demo

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
