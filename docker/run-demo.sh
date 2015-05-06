#!/bin/sh

DIR=$(cd $(dirname "$0") && pwd)
SRC_DIR=${DIR}/..

if [ $(uname) = 'Linux' ]; then
  SUDO=sudo
else
  SUDO=
fi

# make sure the Docker env vars are set
if [ $(uname) = 'Darwin' -o "$1" = '--boot2docker' ]; then
  if [ "$1" = '--boot2docker' ]; then
    shift
  fi
  if grep -q 'lumify-demo' /etc/hosts
  then
    sudo sed -i '.bk' 's/.*lumify-demo.*/'"$(boot2docker ip)"'  lumify-demo/' /etc/hosts
  else
    sudo sh -c "echo \"$(boot2docker ip)  lumify-demo\" >> /etc/hosts"
  fi
  dscacheutil -flushcache
  eval "$(boot2docker shellinit)"
fi

(cd ${DIR} &&
  ${SUDO} docker run \
  -p 2022:22 `# sshd` \
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
  -h lumify-demo \
  lumifyio/demo \
  "$@"
)
