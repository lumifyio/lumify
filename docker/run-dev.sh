#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

SRC_DIR=${DIR}/..

function dir_list {
  echo $1/opt/lumify/{config,lib,logs} \
       $1/var/log/{hadoop,accumulo,elasticsearch} \
       $1/tmp/zookeeper \
       $1/var/lib/hadoop-hdfs \
       $1/var/local/hadoop \
       $1/opt/{elasticsearch/data,rabbitmq/var,jetty/webapps}
}

case $(uname) in
  Linux)
    SUDO=sudo
    PERSISTENT_DIR=${DIR}/lumify-dev-persistent
    mkdir -p $(dir_list ${PERSISTENT_DIR})
    cp ${DIR}/../config/lumify.properties ${PERSISTENT_DIR}/opt/lumify/config/lumify.properties
    cp ${DIR}/../config/log4j.xml ${PERSISTENT_DIR}/opt/lumify/config/log4j.xml
    ;;
  Darwin)
    SUDO=
    dev=$(boot2docker ssh blkid -L boot2docker-data)
    mnt=$(echo "$(boot2docker ssh mount)" | awk -v dev=${dev} '$1 == dev && !seen {print $3; seen = 1}')
    uid=$(boot2docker ssh id -u)
    gid=$(boot2docker ssh id -g)
    PERSISTENT_DIR=${mnt}/lumify-dev-persistent
    boot2docker ssh sudo mkdir -p ${PERSISTENT_DIR}
    boot2docker ssh sudo chown -R ${uid}:${gid} ${PERSISTENT_DIR}
    boot2docker ssh mkdir -p $(dir_list ${PERSISTENT_DIR})
    cat ${DIR}/../config/lumify.properties | boot2docker ssh "cat > ${PERSISTENT_DIR}/opt/lumify/config/lumify.properties"
    cat ${DIR}/../config/log4j.xml | boot2docker ssh "cat > ${PERSISTENT_DIR}/opt/lumify/config/log4j.xml"
    ;;
  *)
    echo "unexpected uname: $(uname)"
    exit -1
    ;;
esac

(cd ${DIR} &&
  ${SUDO} docker run \
  -v ${SRC_DIR}:/opt/lumify-source \
  -v ${PERSISTENT_DIR}/opt/lumify:/opt/lumify \
  -v ${PERSISTENT_DIR}/var/log:/var/log \
  -v ${PERSISTENT_DIR}/tmp:/tmp \
  -v ${PERSISTENT_DIR}/var/lib/hadoop-hdfs:/var/lib/hadoop-hdfs \
  -v ${PERSISTENT_DIR}/var/local/hadoop:/var/local/hadoop \
  -v ${PERSISTENT_DIR}/opt/elasticsearch/data:/opt/elasticsearch/data \
  -v ${PERSISTENT_DIR}/opt/rabbitmq/var:/opt/rabbitmq/var \
  -v ${PERSISTENT_DIR}/opt/jetty/webapps:/opt/jetty/webapps \
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
