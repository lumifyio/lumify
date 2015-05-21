#!/bin/bash -eu

# Add yum repositories
echo "Install yum reppositories"
cp /vagrant/vagrant/config/yum-repos/elasticsearch.repo /etc/yum.repos.d/elasticsearch.repo
cp /vagrant/vagrant/config/yum-repos/lumify.repo /etc/yum.repos.d/lumify.repo

# Install RPM packages
echo "Install RPM packages"
/bin/bash /vagrant/vagrant/scripts/install-rpm-packages.sh

# Install NPM packages
echo "Install NPM packages"
/bin/bash /vagrant/vagrant/scripts/install-npm-packages.sh

# root passwordless SSH
echo "Configure passwordless SSH"
mkdir -p /root/.ssh
cp /vagrant/vagrant/config/ssh_config /root/.ssh/config
chmod 600 /root/.ssh/config && chown root:root /root/.ssh/config
ssh-keygen -q -N "" -t rsa -f /root/.ssh/id_rsa
cat /root/.ssh/id_rsa.pub >> /root/.ssh/authorized_keys

# Install Java
echo "Install Java"
cp /vagrant/vagrant/config/java/java.sh /etc/profile.d/java.sh
source /etc/profile.d/java.sh
/bin/bash /vagrant/vagrant/scripts/install-java.sh

# Install Maven
echo "Install Maven"
echo "export PATH=\$PATH:/opt/maven/bin" >> /etc/profile.d/maven.sh
echo "export MVN_HOME=/opt/maven" >> /etc/profile.d/maven.sh
source /etc/profile.d/maven.sh
/bin/bash /vagrant/vagrant/scripts/install-maven.sh

# Install ZooKeeper
echo "Install ZooKeeper"
echo "export PATH=\$PATH:/opt/zookeeper/bin" >> /etc/profile.d/zookeeper.sh
echo "export ZOOKEEPER_HOME=/opt/zookeeper" >> /etc/profile.d/zookeeper.sh
source /etc/profile.d/zookeeper.sh
/bin/bash /vagrant/vagrant/scripts/install-zookeeper.sh
cp /vagrant/vagrant/config/zookeeper/zookeeper_init.sh /etc/init.d/zookeeper
chmod +x /etc/init.d/zookeeper
chkconfig --add zookeeper
service zookeeper start

# Install Hadoop
echo "Install Hadoop"
mkdir -p /opt/hadoop-2.3.0/etc/hadoop
echo "export HADOOP_PREFIX=/opt/hadoop" >> /etc/profile.d/hadoop.sh
echo "export HADOOP_COMMON_HOME=/opt/hadoop" >> /etc/profile.d/hadoop.sh
echo "export HADOOP_HDFS_HOME=/opt/hadoop" >> /etc/profile.d/hadoop.sh
echo "export HADOOP_MAPRED_HOME=/opt/hadoop" >> /etc/profile.d/hadoop.sh
echo "export HADOOP_YARN_HOME=/opt/hadoop" >> /etc/profile.d/hadoop.sh
echo "export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop" >> /etc/profile.d/hadoop.sh
echo "export YARN_CONF_DIR=/opt/hadoop/etc/hadoop" >> /etc/profile.d/hadoop.sh
echo "export PATH=\$PATH:/opt/hadoop/bin" >> /etc/profile.d/hadoop.sh
source /etc/profile.d/hadoop.sh
cp /vagrant/vagrant/config/hadoop/core-site.xml.template /opt/hadoop-2.3.0/etc/hadoop/core-site.xml.template
cp /vagrant/vagrant/config/hadoop/hadoop-native-64bit.tar.gz /opt/hadoop-2.3.0/hadoop-native-64bit.tar.gz
/bin/bash /vagrant/vagrant/scripts/install-hadoop.sh
cp /vagrant/vagrant/config/hadoop/hdfs-site.xml /opt/hadoop-2.3.0/etc/hadoop/hdfs-site.xml
cp /vagrant/vagrant/config/hadoop/mapred-site.xml /opt/hadoop-2.3.0/etc/hadoop/mapred-site.xml
cp /vagrant/vagrant/config/hadoop/yarn-site.xml /opt/hadoop-2.3.0/etc/hadoop/yarn-site.xml
cp /vagrant/vagrant/config/hadoop/hadoop_init.sh /etc/init.d/hadoop
chmod +x /etc/init.d/hadoop
chkconfig --add hadoop
service hadoop start

# Install Acculmulo
echo "Install Accumulo"
echo "export PATH=\$PATH:/opt/accumulo/bin" >> /etc/profile.d/accumulo.sh
/bin/bash /vagrant/vagrant/scripts/install-accumulo.sh
cp /vagrant/vagrant/config/accumulo/accumulo_init.sh /etc/init.d/accumulo
chmod +x /etc/init.d/accumulo
chkconfig --add accumulo
service accumulo start

# Install ElasticSearch
echo "Install ElasticSearch"
echo "export PATH=\$PATH:/opt/elasticsearch/bin" >> /etc/profile.d/elasticsearch.sh
source /etc/profile.d/elasticsearch.sh
/bin/bash /vagrant/vagrant/scripts/install-elasticsearch.sh
cp /vagrant/vagrant/config/elasticsearch/elasticsearch.yml /opt/elasticsearch/config/elasticsearch.yml
cp /vagrant/vagrant/config/elasticsearch/elasticsearch_init.sh /etc/init.d/elasticsearch
chmod +x /etc/init.d/elasticsearch
chkconfig --add elasticsearch
service elasticsearch start

# Install RabbitMQ
echo "Install RabbitMQ"
echo "export PATH=\$PATH:/opt/rabbitmq/sbin" >> /etc/profile.d/rabbitmq.sh
source /etc/profile.d/rabbitmq.sh
/bin/bash /vagrant/vagrant/scripts/install-rabbitmq.sh
cp /vagrant/vagrant/config/rabbitmq/etc/rabbitmq/rabbitmq.config /opt/rabbitmq_server-3.4.1/etc/rabbitmq/rabbitmq.config
cp /vagrant/vagrant/config/rabbitmq/rabbitmq_init.sh /etc/init.d/rabbitmq
chmod +x /etc/init.d/rabbitmq
chkconfig --add rabbitmq
service rabbitmq start

# Install Jetty
echo "Install Jetty"
echo "export PATH=\$PATH:/opt/jetty/bin" >> /etc/profile.d/jetty.sh
echo "export JETTY_HOME=/opt/jetty" >> /etc/profile.d/jetty.sh
source /etc/profile.d/jetty.sh
/bin/bash /vagrant/vagrant/scripts/install-jetty.sh
cp /vagrant/vagrant/config/jetty/start.ini /opt/jetty/start.ini
cp /vagrant/vagrant/config/jetty/jetty-logging.properties /opt/jetty/resources/jetty-logging.properties
cp /vagrant/vagrant/config/jetty/jetty.xml /opt/jetty/etc/jetty.xml
cp /vagrant/vagrant/config/jetty/jetty-http.xml /opt/jetty/etc/jetty-http.xml
cp /vagrant/vagrant/config/jetty/jetty-https.xml /opt/jetty/etc/jetty-https.xml
cp /vagrant/vagrant/config/jetty/jetty-ssl.xml /opt/jetty/etc/jetty-ssl.xml
cp /vagrant/vagrant/config/jetty/jetty.jks /opt/jetty/etc/jetty.jks
cp /vagrant/vagrant/config/jetty/jetty_init.sh /etc/init.d/jetty
chmod +x /etc/init.d/jetty

# create a link to the opencv_java lib where jetty can find it
ln -s /usr/local/share/OpenCV/java/libopencv_java249.so /usr/lib/libopencv_java249.so
ldconfig

# turn iptables firewall off
/etc/init.d/iptables stop
/sbin/chkconfig iptables off

# configure Lumify directories in HDFS
/opt/hadoop/bin/hadoop fs -mkdir -p /lumify/libcache
/opt/hadoop/bin/hadoop fs -mkdir -p /lumify/config/opencv
/opt/hadoop/bin/hadoop fs -mkdir -p /lumify/config/opennlp
/opt/hadoop/bin/hadoop fs -mkdir -p /lumify/config/knownEntities/dictionaries
/opt/hadoop/bin/hadoop fs -put /vagrant/config/opencv/haarcascade_frontalface_alt.xml /lumify/config/opencv/
/opt/hadoop/bin/hadoop fs -put /vagrant/config/opennlp/* /lumify/config/opennlp/
/opt/hadoop/bin/hadoop fs -put /vagrant/config/knownEntities/dictionaries/* /lumify/config/knownEntities/dictionaries/
/opt/hadoop/bin/hadoop fs -chmod -R a+w /lumify/


