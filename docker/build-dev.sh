#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
cd ${DIR}

case $(uname) in
  Linux)
    (cd ${DIR} && sudo docker build -t lumifyio/dev dev)
    ;;
  Darwin)
    (cd ${DIR} && docker build -t lumifyio/dev dev)
    ;;
  *)
    echo "unexpected uname: $(uname)"
    exit -1
    ;;
esac
