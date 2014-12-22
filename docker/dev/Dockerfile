
FROM centos:centos6

# Add yum repositories
ADD yum-repos/elasticsearch.repo /etc/yum.repos.d/elasticsearch.repo

# Install RPM packages
ADD scripts/install-rpm-packages.sh /opt/lumify/scripts/install-rpm-packages.sh
RUN /bin/bash /opt/lumify/scripts/install-rpm-packages.sh

# Install NPM packages
ADD scripts/install-npm-packages.sh /opt/lumify/scripts/install-npm-packages.sh
RUN /bin/bash /opt/lumify/scripts/install-npm-packages.sh

# Passwordless SSH
ADD scripts/install-ssh.sh /opt/lumify/scripts/install-ssh.sh
ADD ssh_config /root/.ssh/config
RUN /bin/bash /opt/lumify/scripts/install-ssh.sh
EXPOSE 22

# Install Java
ADD scripts/install-java.sh /opt/lumify/scripts/install-java.sh
ADD java/java.sh /etc/profile.d/java.sh
ENV PATH $PATH:/opt/jdk/bin
ENV JAVA_HOME /opt/jdk
ENV _JAVA_OPTIONS -Djava.net.preferIPv4Stack=true
RUN /bin/bash /opt/lumify/scripts/install-java.sh

# Install Maven
ADD scripts/install-maven.sh /opt/lumify/scripts/install-maven.sh
ENV PATH $PATH:/opt/maven/bin
ENV MVN_HOME /opt/maven
RUN /bin/bash /opt/lumify/scripts/install-maven.sh

# Install ZooKeeper
ADD scripts/install-zookeeper.sh /opt/lumify/scripts/install-zookeeper.sh
ENV PATH $PATH:/opt/zookeeper/bin
ENV ZOOKEEPER_HOME /opt/zookeeper
RUN /bin/bash /opt/lumify/scripts/install-zookeeper.sh
EXPOSE 2181 2888 3888

# Install Hadoop
ADD scripts/install-hadoop.sh /opt/lumify/scripts/install-hadoop.sh
ADD hadoop/core-site.xml.template /opt/hadoop-2.3.0/etc/hadoop/core-site.xml.template
ADD hadoop/hdfs-site.xml /opt/hadoop-2.3.0/etc/hadoop/hdfs-site.xml
ADD hadoop/mapred-site.xml /opt/hadoop-2.3.0/etc/hadoop/mapred-site.xml
ADD hadoop/yarn-site.xml /opt/hadoop-2.3.0/etc/hadoop/yarn-site.xml
ENV HADOOP_PREFIX /opt/hadoop
ENV HADOOP_COMMON_HOME /opt/hadoop
ENV HADOOP_HDFS_HOME /opt/hadoop
ENV HADOOP_MAPRED_HOME /opt/hadoop
ENV HADOOP_YARN_HOME /opt/hadoop
ENV HADOOP_CONF_DIR /opt/hadoop/etc/hadoop
ENV YARN_CONF_DIR /opt/hadoop/etc/hadoop
ENV PATH $PATH:/opt/hadoop/bin
RUN /bin/bash /opt/lumify/scripts/install-hadoop.sh
VOLUME ["/var/lib/hadoop-hdfs", "/var/local/hadoop"]
EXPOSE 8020 8032 8088 9000 50010 50020 50030 50060 50070 50075 50090

# Install Accumulo
ADD scripts/install-accumulo.sh /opt/lumify/scripts/install-accumulo.sh
ENV PATH $PATH:/opt/accumulo/bin
RUN /bin/bash /opt/lumify/scripts/install-accumulo.sh
EXPOSE 9997 9999 50091 50095

# Install ElasticSearch
ADD scripts/install-elasticsearch.sh /opt/lumify/scripts/install-elasticsearch.sh
ENV PATH $PATH:/opt/elasticsearch/bin
RUN /bin/bash /opt/lumify/scripts/install-elasticsearch.sh
VOLUME ["/opt/elasticsearch-1.4.1/data/"]
EXPOSE 9200 9300

# Install RabbitMQ
ADD scripts/install-rabbitmq.sh /opt/lumify/scripts/install-rabbitmq.sh
ADD rabbitmq/etc/rabbitmq/rabbitmq.config /opt/rabbitmq_server-3.4.1/etc/rabbitmq/rabbitmq.config
ENV PATH $PATH:/opt/rabbitmq/sbin
RUN /bin/bash /opt/lumify/scripts/install-rabbitmq.sh
VOLUME ["/opt/rabbitmq_server-3.4.1/var"]
EXPOSE 5672 5673 15672

# Install Jetty
ADD scripts/install-jetty.sh /opt/lumify/scripts/install-jetty.sh
RUN /bin/bash /opt/lumify/scripts/install-jetty.sh
ADD jetty/start.ini /opt/jetty/start.ini
ADD jetty/jetty-logging.properties /opt/jetty/resources/jetty-logging.properties
ADD jetty/jetty.xml /opt/jetty/etc/jetty.xml
ADD jetty/jetty-http.xml /opt/jetty/etc/jetty-http.xml
ADD jetty/jetty-https.xml /opt/jetty/etc/jetty-https.xml
ADD jetty/jetty-ssl.xml /opt/jetty/etc/jetty-ssl.xml
ADD jetty/jetty.jks /opt/jetty/etc/jetty.jks
ENV PATH $PATH:/opt/jetty/bin
VOLUME ["/opt/jetty/webapps"]
EXPOSE 8080 8443

# add scripts
ADD start.sh /opt/start.sh
ADD stop.sh /opt/stop.sh

RUN \
  chmod +x /opt/start.sh

# Set working environment
VOLUME ["/tmp", "/var/log", "/opt/lumify", "/opt/lumify-source"]
WORKDIR /home/root/lumify
CMD ["/opt/start.sh", "-d"]
