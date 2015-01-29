#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)

if [ $(uname) = 'Darwin' -o "$1" = '--boot2docker' ]; then
  dev=$(boot2docker ssh blkid -L boot2docker-data)
  mnt=$(echo "$(boot2docker ssh mount)" | awk -v dev=${dev} '$1 == dev && !seen {print $3; seen = 1}')
  uid=$(boot2docker ssh id -u)
  gid=$(boot2docker ssh id -g)
  PERSISTENT_DIR=${mnt}/lumify-dev-persistent
  boot2docker ssh sudo rm -rf ${PERSISTENT_DIR}
  LOCAL_PERSISTENT_DIR=${DIR}/lumify-dev-persistent
  rm -rf ${LOCAL_PERSISTENT_DIR}
else
  PERSISTENT_DIR=${DIR}/lumify-dev-persistent
  rm -rf ${PERSISTENT_DIR}
fi
