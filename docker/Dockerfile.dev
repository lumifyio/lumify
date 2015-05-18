
FROM centos:centos6

# Add yum repositories
ADD config/yum-repos/elasticsearch.repo /etc/yum.repos.d/elasticsearch.repo

# Install RPM packages
ADD scripts/install-rpm-packages.sh /opt/lumify/scripts/install-rpm-packages.sh
RUN /bin/bash /opt/lumify/scripts/install-rpm-packages.sh

# Install NPM packages
ADD scripts/install-npm-packages.sh /opt/lumify/scripts/install-npm-packages.sh
RUN /bin/bash /opt/lumify/scripts/install-npm-packages.sh

# Passwordless SSH
ADD scripts/install-ssh.sh /opt/lumify/scripts/install-ssh.sh
ADD config/ssh_config /root/.ssh/config
RUN /bin/bash /opt/lumify/scripts/install-ssh.sh
EXPOSE 22

# Install Java
ADD scripts/install-java.sh /opt/lumify/scripts/install-java.sh
ADD config/java/java.sh /etc/profile.d/java.sh
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
ADD config/hadoop/core-site.xml.template /opt/hadoop-2.3.0/etc/hadoop/core-site.xml.template
COPY config/hadoop/hadoop-native-64bit.tar.gz /opt/hadoop-2.3.0/hadoop-native-64bit.tar.gz
ENV HADOOP_PREFIX /opt/hadoop
ENV HADOOP_COMMON_HOME /opt/hadoop
ENV HADOOP_HDFS_HOME /opt/hadoop
ENV HADOOP_MAPRED_HOME /opt/hadoop
ENV HADOOP_YARN_HOME /opt/hadoop
ENV HADOOP_CONF_DIR /opt/hadoop/etc/hadoop
ENV YARN_CONF_DIR /opt/hadoop/etc/hadoop
ENV PATH $PATH:/opt/hadoop/bin
RUN /bin/bash /opt/lumify/scripts/install-hadoop.sh
ADD config/hadoop/hdfs-site.xml /opt/hadoop-2.3.0/etc/hadoop/hdfs-site.xml
ADD config/hadoop/mapred-site.xml /opt/hadoop-2.3.0/etc/hadoop/mapred-site.xml
ADD config/hadoop/yarn-site.xml /opt/hadoop-2.3.0/etc/hadoop/yarn-site.xml
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
ADD config/elasticsearch/elasticsearch.yml /opt/elasticsearch/config/elasticsearch.yml
VOLUME ["/opt/elasticsearch-1.4.4/data/"]
EXPOSE 9200 9300

# Install RabbitMQ
ADD scripts/install-rabbitmq.sh /opt/lumify/scripts/install-rabbitmq.sh
ENV PATH $PATH:/opt/rabbitmq/sbin
RUN /bin/bash /opt/lumify/scripts/install-rabbitmq.sh
ADD config/rabbitmq/etc/rabbitmq/rabbitmq.config /opt/rabbitmq_server-3.4.1/etc/rabbitmq/rabbitmq.config
VOLUME ["/opt/rabbitmq_server-3.4.1/var"]
EXPOSE 5672 5673 15672

# Install Jetty
ADD scripts/install-jetty.sh /opt/lumify/scripts/install-jetty.sh
RUN /bin/bash /opt/lumify/scripts/install-jetty.sh
ADD config/jetty/start.ini /opt/jetty/start.ini
ADD config/jetty/jetty-logging.properties /opt/jetty/resources/jetty-logging.properties
ADD config/jetty/jetty.xml /opt/jetty/etc/jetty.xml
ADD config/jetty/jetty-http.xml /opt/jetty/etc/jetty-http.xml
ADD config/jetty/jetty-https.xml /opt/jetty/etc/jetty-https.xml
ADD config/jetty/jetty-ssl.xml /opt/jetty/etc/jetty-ssl.xml
ADD config/jetty/jetty.jks /opt/jetty/etc/jetty.jks
ENV PATH $PATH:/opt/jetty/bin
VOLUME ["/opt/jetty/webapps"]
EXPOSE 8080 8443

# add scripts
ADD scripts/docker-entrypoint.sh /opt/docker-entrypoint.sh
ADD dev/start.sh /opt/start.sh
ADD dev/stop.sh /opt/stop.sh
ADD dev/status.sh /opt/status.sh

RUN \
  chmod +x /opt/start.sh \
  && chmod +x /opt/stop.sh \
  && chmod +x /opt/status.sh \
  && chmod +x /opt/docker-entrypoint.sh

# Set working environment
VOLUME ["/tmp", "/var/log", "/opt/lumify", "/opt/lumify-source"]
WORKDIR /home/root/lumify
ENTRYPOINT ["/opt/docker-entrypoint.sh"]
CMD ["start"]
