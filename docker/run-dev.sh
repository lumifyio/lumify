#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)

SRC_DIR=${DIR}/..

dir_list() {
  echo $1/opt/lumify/config \
       $1/opt/lumify/lib \
       $1/opt/lumify/logs \
       $1/var/log/hadoop \
       $1/var/log/accumulo \
       $1/var/log/elasticsearch \
       $1/tmp/zookeeper \
       $1/var/lib/hadoop-hdfs \
       $1/var/local/hadoop \
       $1/opt/elasticsearch/data \
       $1/opt/rabbitmq/var \
       $1/opt/jetty/webapps
}

if [ $(uname) = 'Linux' ]; then
  SUDO=sudo
else
  SUDO=
fi

if [ "$1" = '--boot2docker' ]; then
  shift
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
  ${BOOT2DOCKER_SSH} sudo mkdir -p ${PERSISTENT_DIR}
  ${BOOT2DOCKER_SSH} sudo chown -R ${uid}:${gid} ${PERSISTENT_DIR}
  ${BOOT2DOCKER_SSH} mkdir -p $(dir_list ${PERSISTENT_DIR})
  LOCAL_PERSISTENT_DIR=${DIR}/lumify-dev-persistent
  mkdir -p $(dir_list ${LOCAL_PERSISTENT_DIR})
  touch ${LOCAL_PERSISTENT_DIR}/NOT_ALL_OF_YOUR_FILES_ARE_HERE
  touch ${LOCAL_PERSISTENT_DIR}/OTHER_FILES_ARE_PERSISTED_INSIDE_THE_BOOT2DOCKER_VM
else
  PERSISTENT_DIR=${DIR}/lumify-dev-persistent
  mkdir -p $(dir_list ${PERSISTENT_DIR})
  LOCAL_PERSISTENT_DIR=${DIR}/lumify-dev-persistent
fi

cp ${DIR}/../config/lumify.properties ${LOCAL_PERSISTENT_DIR}/opt/lumify/config/lumify.properties
cp ${DIR}/../config/log4j.xml ${LOCAL_PERSISTENT_DIR}/opt/lumify/config/log4j.xml

(cd ${DIR} &&
  ${SUDO} docker run \
  -v ${SRC_DIR}:/opt/lumify-source \
  -v ${PERSISTENT_DIR}/var/log:/var/log \
  -v ${PERSISTENT_DIR}/tmp:/tmp \
  -v ${PERSISTENT_DIR}/var/lib/hadoop-hdfs:/var/lib/hadoop-hdfs \
  -v ${PERSISTENT_DIR}/var/local/hadoop:/var/local/hadoop \
  -v ${PERSISTENT_DIR}/opt/elasticsearch/data:/opt/elasticsearch/data \
  -v ${PERSISTENT_DIR}/opt/rabbitmq/var:/opt/rabbitmq/var \
  -v ${LOCAL_PERSISTENT_DIR}/opt/lumify:/opt/lumify \
  -v ${LOCAL_PERSISTENT_DIR}/opt/jetty/webapps:/opt/jetty/webapps \
  -p 2181:2181 `# ZooKeeper` \
  -p 5672:5672 `# RabbitMQ` \
  -p 5673:5673 `# RabbitMQ` \
  -p 8020:8020 `# Hadoop: HDFS` \
  -p 8032:8032 `# Hadoop: Resource Manager` \
  -p 8042:8042 `# Hadoop: Node Manager: Web UI` \
  -p 8080:8080 `# Jetty HTTP` \
  -p 8088:8088 `# Hadoop: Resource Manager: Web UI` \
  -p 8443:8443 `# Jetty HTTPS` \
  -p 9000:9000 `# Hadoop: Name Node: Metadata Service` \
  -p 9200:9200 `# Elasticsearch` \
  -p 9300:9300 `# Elasticsearch` \
  -p 9997:9997 `# Accumulo` \
  -p 9999:9999 `# Accumulo` \
  -p 15672:15672 `# RabbitMQ: Web UI` \
  -p 50010:50010 `# Hadoop: Data Node: Data transfer` \
  -p 50020:50020 `# Hadoop: Data Node: Metadata operations` \
  -p 50030:50030 `# Hadoop: Job Tracker` \
  -p 50060:50060 `# Hadoop: Task Tracker: Web UI` \
  -p 50070:50070 `# Hadoop: Name Node: Web UI` \
  -p 50075:50075 `# Hadoop: Data Node: Web UI` \
  -p 50090:50090 `# Hadoop: Secondary Name Node` \
  -p 50095:50095 `# Accumulo: Web UI` \
  -i \
  -t \
  -h lumify-dev \
  lumifyio/dev \
  "$@"
)
