#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)

if [ $(uname) = 'Darwin' -o "$1" = '--boot2docker' ]; then
  if [ "$1" = '--boot2docker' ]; then
    shift
  fi
  SPLIT_PERSISTENT_DIR='true'

  which boot2docker > /dev/null
  if [ $? -eq 0 ]; then
    BOOT2DOCKER_SSH='boot2docker ssh'
  else
    BOOT2DOCKER_SSH=
  fi
fi

if [ $(uname) = 'Darwin' -o "${SPLIT_PERSISTENT_DIR}" = 'true' ]; then
  dev=$(${BOOT2DOCKER_SSH} blkid -L boot2docker-data)
  mnt=$(echo "$(${BOOT2DOCKER_SSH} mount)" | awk -v dev=${dev} '$1 == dev && !seen {print $3; seen = 1}')
  uid=$(${BOOT2DOCKER_SSH} id -u)
  gid=$(${BOOT2DOCKER_SSH} id -g)
  PERSISTENT_DIR=${mnt}/lumify-dev-persistent
  ${BOOT2DOCKER_SSH} sudo rm -rf ${PERSISTENT_DIR}
  LOCAL_PERSISTENT_DIR=${DIR}/dev/lumify-dev-persistent
  rm -rf ${LOCAL_PERSISTENT_DIR}
else
  PERSISTENT_DIR=${DIR}/dev/lumify-dev-persistent
  rm -rf ${PERSISTENT_DIR}
fi
