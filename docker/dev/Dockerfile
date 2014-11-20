
FROM centos:centos6

RUN \
  rpm -Uhv http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm \
  && yum update -y \
  && yum install -y wget curl tar sudo openssh-server openssh-clients git nodejs npm libuuid-devel libtool zip unzip rsync which

RUN \
  npm install -g inherits bower grunt grunt-cli

# Passwordless SSH
RUN \
  ssh-keygen -q -N "" -t dsa -f /etc/ssh/ssh_host_dsa_key \
  && ssh-keygen -q -N "" -t rsa -f /etc/ssh/ssh_host_rsa_key \
  && ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa \
  && cp /root/.ssh/id_rsa.pub /root/.ssh/authorized_keys

ADD ssh_config /root/.ssh/config
RUN chmod 600 /root/.ssh/config
RUN chown root:root /root/.ssh/config

EXPOSE 22

# Install Java
RUN \
  wget --header "Cookie: oraclelicense=accept-securebackup-cookie" -O /opt/jdk-7u71-linux-x64.tar.gz http://download.oracle.com/otn-pub/java/jdk/7u71-b14/jdk-7u71-linux-x64.tar.gz \
  && tar -xzf /opt/jdk-7u71-linux-x64.tar.gz -C /opt \
  && rm /opt/jdk-7u71-linux-x64.tar.gz \
  && ln -s /opt/jdk1.7.0_71 /opt/jdk

ENV PATH $PATH:/opt/jdk/bin
ENV JAVA_HOME /opt/jdk
ENV _JAVA_OPTIONS -Djava.net.preferIPv4Stack=true

# Install Maven
RUN \
  wget -O /opt/apache-maven-3.2.3-bin.tar.gz http://apache.mirrors.pair.com/maven/maven-3/3.2.3/binaries/apache-maven-3.2.3-bin.tar.gz \
  && tar -xzf /opt/apache-maven-3.2.3-bin.tar.gz -C /opt \
  && rm /opt/apache-maven-3.2.3-bin.tar.gz \
  && ln -s /opt/apache-maven-3.2.3 /opt/maven

ENV PATH $PATH:/opt/maven/bin
ENV MVN_HOME /opt/maven

# Install ZooKeeper
RUN \
  wget -O /opt/zookeeper-3.4.6.tar.gz http://apache.mirrors.pair.com/zookeeper/zookeeper-3.4.6/zookeeper-3.4.6.tar.gz \
  && tar -xzf /opt/zookeeper-3.4.6.tar.gz -C /opt \
  && rm /opt/zookeeper-3.4.6.tar.gz \
  && ln -s /opt/zookeeper-3.4.6 /opt/zookeeper \
  && cp /opt/zookeeper/conf/zoo_sample.cfg /opt/zookeeper/conf/zoo.cfg \
  && mkdir -p /tmp/zookeeper

ENV PATH $PATH:/opt/zookeeper/bin

EXPOSE 2181 2888 3888

# Install Hadoop
VOLUME ["/var/lib/hadoop-hdfs", "/var/local/hadoop", "/opt/hadoop-2.5.1/logs"]

RUN \
  wget -O /opt/hadoop-2.5.1.tar.gz http://apache.mirrors.pair.com/hadoop/common/hadoop-2.5.1/hadoop-2.5.1.tar.gz \
  && tar -xzf /opt/hadoop-2.5.1.tar.gz -C /opt/ \
  && rm /opt/hadoop-2.5.1.tar.gz \
  && ln -s /opt/hadoop-2.5.1 /opt/hadoop \
  && sed -i '/^export JAVA_HOME/ s:.*:export JAVA_HOME=/opt/jdk\nexport HADOOP_PREFIX=/opt/hadoop\nexport HADOOP_HOME=/opt/hadoop\n:' /opt/hadoop/etc/hadoop/hadoop-env.sh \
  && sed -i '/^export HADOOP_CONF_DIR/ s:.*:export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop/:' /opt/hadoop/etc/hadoop/hadoop-env.sh \
  && sed -i '/^hadoop.log.dir=.*/ s:.*:hadoop.log.dir=/var/log/hadoop:' /opt/hadoop/etc/hadoop/log4j.properties \
  && mkdir /opt/hadoop/input \
  && mkdir /var/log/hadoop \
  && rm /opt/hadoop/lib/native/* \
  && wget -q -O - http://dl.bintray.com/sequenceiq/sequenceiq-bin/hadoop-native-64-2.5.0.tar | tar -xzf - -C /opt/hadoop/lib/native/ \
  && chmod +x /opt/hadoop/etc/hadoop/*-env.sh

ADD hadoop/core-site.xml.template /opt/hadoop-2.5.1/etc/hadoop/core-site.xml.template
ADD hadoop/hdfs-site.xml /opt/hadoop-2.5.1/etc/hadoop/hdfs-site.xml
ADD hadoop/mapred-site.xml /opt/hadoop-2.5.1/etc/hadoop/mapred-site.xml
ADD hadoop/yarn-site.xml /opt/hadoop-2.5.1/etc/hadoop/yarn-site.xml

ENV HADOOP_PREFIX /opt/hadoop
ENV HADOOP_COMMON_HOME /opt/hadoop
ENV HADOOP_HDFS_HOME /opt/hadoop
ENV HADOOP_MAPRED_HOME /opt/hadoop
ENV HADOOP_YARN_HOME /opt/hadoop
ENV HADOOP_CONF_DIR /opt/hadoop/etc/hadoop
ENV YARN_CONF_DIR /opt/hadoop/etc/hadoop
ENV PATH $PATH:/opt/hadoop/bin

RUN \
  cp /opt/hadoop/etc/hadoop/*.xml /opt/hadoop/input \
  && sed s/HOSTNAME/localhost/ /opt/hadoop/etc/hadoop/core-site.xml.template > /opt/hadoop/etc/hadoop/core-site.xml

RUN \
  /opt/hadoop/bin/hdfs namenode -format

EXPOSE 8032 8088 50030 50060 50070 50075 50090 50105

# Install Accumulo
VOLUME ["/opt/accumulo-1.6.1/logs"]

RUN \
  wget -O /opt/accumulo-1.6.1-bin.tar.gz http://apache.mirrors.pair.com/accumulo/1.6.1/accumulo-1.6.1-bin.tar.gz \
  && tar -xzf /opt/accumulo-1.6.1-bin.tar.gz -C /opt \
  && rm /opt/accumulo-1.6.1-bin.tar.gz \
  && ln -s /opt/accumulo-1.6.1 /opt/accumulo \
  && cp /opt/accumulo/conf/examples/1GB/standalone/* /opt/accumulo/conf \
  && sed -i -e "s|HADOOP_PREFIX=/path/to/hadoop|HADOOP_PREFIX=/opt/hadoop|" \
            -e "s|JAVA_HOME=/path/to/java|JAVA_HOME=/opt/jdk|" \
            -e "s|ZOOKEEPER_HOME=/path/to/zookeeper|ZOOKEEPER_HOME=/opt/zookeeper|" \
            -e "s|.*export ACCUMULO_MONITOR_BIND_ALL.*|export ACCUMULO_MONITOR_BIND_ALL=\"true\"|" \
            /opt/accumulo/conf/accumulo-env.sh \
  && sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/masters \
  && sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/slaves \
  && sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/tracers \
  && sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/gc \
  && sed -i -e "s|localhost|0.0.0.0|" /opt/accumulo/conf/monitor

ENV PATH $PATH:/opt/accumulo/bin
EXPOSE 9997 9999 50091 50095

# Install ElasticSearch
VOLUME ["/opt/elasticsearch-1.4.0/logs"]

RUN \
  wget -O /opt/elasticsearch-1.4.0.tar.gz https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.4.0.tar.gz \
  && tar -xzf /opt/elasticsearch-1.4.0.tar.gz -C /opt \
  && rm /opt/elasticsearch-1.4.0.tar.gz \
  && ln -s /opt/elasticsearch-1.4.0 /opt/elasticsearch \
  && /opt/elasticsearch/bin/plugin -install mobz/elasticsearch-head \
  && /opt/elasticsearch/bin/plugin install org.securegraph/securegraph-elasticsearch-plugin/0.6.0

ENV PATH $PATH:/opt/elasticsearch/bin
EXPOSE 9200 9300

# add scripts
ADD start.sh /opt/start.sh
ADD stop.sh /opt/stop.sh

RUN \
  chmod +x /opt/start.sh

# Set working environment
VOLUME ["/tmp", "/var/log", "/home/root/lumify"]
WORKDIR /home/root/lumify
CMD ["/opt/start.sh", "-d"]
