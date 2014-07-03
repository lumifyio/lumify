# Lumify on Ubuntu 14


## Install Dependencies

### C compiler, other build tools, and Git

*as root:*

        aptitude install -y build-essential git


### Java 6

1. browse to http://www.oracle.com/technetwork/java/javase/downloads/java-archive-downloads-javase6-419409.html#jdk-6u45-oth-JPR
1. accept the license agreement
1. select jdk-6u45-linux-x64.bin
1. login and the JDK will download
1. move `jdk-6u45-linux-x64.bin` to `/opt`

*as root:*

        cd /opt
        chmod u+x jdk-6u45-linux-x64.bin
        ./jdk-6u45-linux-x64.bin
        ln -s jdk1.6.0_45 jdk


### Maven

*as root:*

        export JAVA_HOME=/opt/jdk
        export PATH=$JAVA_HOME/bin:$PATH
        aptitude install -y maven


### NodeJS and NPM modules

*as root:*

        cd ~
        curl http://nodejs.org/dist/v0.10.29/node-v0.10.29.tar.gz -O
        tar xzf node-v0.10.29.tar.gz
        cd node-v0.10.29
        ./configure && make && make install

        npm install -g bower
        npm install -g grunt-cli


## Install Servers

### Hadoop

*as root:*

        groupadd hadoop
        useradd -g hadoop hdfs
        useradd -g hadoop mapred

        cd ~
        curl http://www.us.apache.org/dist/hadoop/common/hadoop-0.23.11/hadoop-0.23.11.tar.gz -O
        cd /usr/lib
        tar xzf ~/hadoop-0.23.11.tar.gz
        ln -s hadoop-0.23.11 hadoop
        chown -R root:hadoop hadoop/

        rm -rf /var/{lib,log,run}/hadoop
        mkdir -p /var/lib/hadoop/hdfs/{namenode,datanode} /var/lib/hadoop/mapred /var/{log,run}/hadoop
        chmod -R 755 /var/lib/hadoop
        chown -R hdfs:hadoop /var/lib/hadoop/hdfs
        chown -R mapred:hadoop /var/lib/hadoop/mapred

        ip_address=$(ip addr show eth0 | awk '/inet / {print $2}' | cut -d / -f 1)
        hadoop/sbin/hadoop-setup-conf.sh --auto \
                                         --hdfs-user=hdfs \
                                         --mapreduce-user=mapred \
                                         --conf-dir=/usr/lib/hadoop/etc/hadoop \
                                         --datanode-dir=/var/lib/hadoop/hdfs/datanode \
                                         --hdfs-dir=/var/lib/hadoop/hdfs \
                                         --jobtracker-host=${ip_address} \
                                         --log-dir=/var/log/hadoop \
                                         --pid-dir=/var/run/hadoop \
                                         --mapred-dir=/var/lib/hadoop/mapred \
                                         --namenode-dir=/var/lib/hadoop/hdfs/namenode \
                                         --namenode-host=${ip_address} \
                                         --replication=1
        echo ${ip_address} > hadoop/etc/hadoop/slaves

        sed -i -e "/$(hostname)/ d" /etc/hosts
        echo "${ip_address} $(hostname)" >> /etc/hosts

        sed -i -e 's|JAVA_HOME=${JAVA_HOME}|JAVA_HOME=/opt/jdk|' \
               -e 's|HADOOP_CONF_DIR=${HADOOP_CONF_DIR:-"/etc/hadoop"}|HADOOP_CONF_DIR=/usr/lib/hadoop/etc/hadoop|' \
          hadoop/etc/hadoop/hadoop-env.sh

        vi hadoop/etc/hadoop/hdfs-site.xml
        # add:
          <property>
              <name>dfs.permissions.enabled</name>
              <value>false</value>
          </property>
          <property>
              <name>dfs.datanode.synconclose</name>
              <value>true</value>
          </property>

        ssh-keygen -b 2048 -t RSA
        # accept the default location, press enter twice for no passphrase
        cat ~/.ssh/id_rsa.pub >> ~/.ssh/authorized_keys


### Zookeeper

*as root:*

        cd ~
        curl http://www.us.apache.org/dist/zookeeper/zookeeper-3.4.5/zookeeper-3.4.5.tar.gz -O
        tar xzf ~/zookeeper-3.4.5.tar.gz
        ln -s zookeeper-3.4.5 zookeeper
        cp zookeeper/conf/zoo_sample.cfg zookeeper/conf/zoo.cfg


### Accumulo

*as root:*

        cd ~
        curl http://www.us.apache.org/dist/accumulo/1.5.1/accumulo-1.5.1-bin.tar.gz -O
        cd /usr/lib
        tar xzf ~/accumulo-1.5.1-bin.tar.gz
        ln -s accumulo-1.5.1 accumulo
        cp accumulo/conf/examples/1GB/standalone/* accumulo/conf

        sed -i -e "s|HADOOP_PREFIX=/path/to/hadoop|HADOOP_PREFIX=/usr/lib/hadoop|" \
               -e "s|JAVA_HOME=/path/to/java|JAVA_HOME=/opt/jdk|" \
               -e "s|ZOOKEEPER_HOME=/path/to/zookeeper|ZOOKEEPER_HOME=/usr/lib/zookeeper|" \
          accumulo/conf/accumulo-env.sh

        vi accumulo/conf/accumulo-site.xml
        # follow the instructions in the comment in the general.classpaths property
        # add:
          <property>
              <name>instance.dfs.uri</name>
              <value>hdfs://<%= @namenode_hostname %>:8020</value>
           </property>

        ip_address=$(ip addr show eth0 | awk '/inet / {print $2}' | cut -d / -f 1)
        sed -i -e "s/192.168.33.10/${ip_address}/" accumulo/conf/accumulo-site.xml
        sed -i -e "s/localhost/${ip_address}/" accumulo/conf/monitor


### Elasticsearch

*as root:*

        cd ~
        curl https://download.elasticsearch.org/elasticsearch/elasticsearch/elasticsearch-1.1.2.deb -O
        dpkg -i elasticsearch-1.1.2.deb
        /usr/share/elasticsearch/bin/plugin install org.securegraph/securegraph-elasticsearch-plugin/0.6.0
        /usr/share/elasticsearch/bin/plugin install mobz/elasticsearch-head


### Storm

*as root:*

        cd ~
        curl http://www.us.apache.org/dist/incubator/storm/apache-storm-0.9.2-incubating/apache-storm-0.9.2-incubating.tar.gz -O
        cd /opt
        tar xzf ~/apache-storm-0.9.2-incubating.tar.gz 
        ln -s apache-storm-0.9.2-incubating storm

        ip_address=$(ip addr show eth0 | awk '/inet / {print $2}' | cut -d / -f 1)
        echo "storm.zookeeper.servers:" >> /opt/storm/conf/storm.yaml
        echo " - ${ip_address}" >> /opt/storm/conf/storm.yaml
        echo "nimbus.host: ${ip_address}" >> /opt/storm/conf/storm.yaml
        echo "supervisor.slots.ports: [6700, 6701, 6702, 6703]" >> /opt/storm/conf/storm.yaml
        echo "ui.port: 8081" >> /opt/storm/conf/storm.yaml


### RabbitMQ

*as root:*

        aptitude install -y rabbitmq-server
        /usr/sbin/rabbitmq-plugins enable rabbitmq_management


### Jetty with SSL support

*as root:*

        cd ~
        curl -L 'http://eclipse.org/downloads/download.php?file=/jetty/stable-8/dist/jetty-distribution-8.1.15.v20140411.tar.gz&r=1' \
             -o jetty-distribution-8.1.15.v20140411.tar.gz
        cd /opt
        tar xzf ~/jetty-distribution-8.1.15.v20140411.tar.gz
        ln -s jetty-distribution-8.1.15.v20140411 jetty

        mkdir -p /opt/lumify/config
        keytool -genkeypair -keysize 2048 -keyalg RSA -keypass password -storepass password \
                -dname CN=lumify -keystore /opt/lumify/config/jetty.jks

        vi jetty/etc/jetty.xml
        # add the following after the existing addConnector call
          <Call name="addConnector">
            <Arg>
              <New class="org.eclipse.jetty.server.ssl.SslSelectChannelConnector">
                <Arg>
                  <New class="org.eclipse.jetty.http.ssl.SslContextFactory">
                    <Set name="keyStore">/opt/lumify/config/jetty.jks</Set>
                    <Set name="keyStorePassword">password</Set>
                    <Set name="trustStore">/opt/lumify/config/jetty.jks</Set>
                    <Set name="trustStorePassword">password</Set>
                    <Set name="needClientAuth">false</Set>
                  </New>
                </Arg>
                <Set name="host"><Property name="jetty.host" /></Set>
                <Set name="port"><Property name="jetty.port" default="8443"/></Set>
                <Set name="maxIdleTime">300000</Set>
                <Set name="Acceptors">2</Set>
                <Set name="statsOn">false</Set>
                <Set name="lowResourcesConnections">20000</Set>
                <Set name="lowResourcesMaxIdleTime">5000</Set>
              </New>
            </Arg>
          </Call>


## Setup

### format and initialize Hadoop HDFS and Accumulo

*as root:*

        /usr/lib/hadoop/bin/hdfs namenode -format
        /usr/lib/hadoop/sbin/start-dfs.sh
        /usr/lib/zookeeper/bin/zkServer.sh start
        /usr/lib/accumulo/bin/accumulo init --instance-name lumify --password password


### start services

*as root:*

        /usr/lib/hadoop/sbin/start-dfs.sh
        /usr/lib/zookeeper/bin/zkServer.sh start
        /usr/lib/accumulo/bin/start-all.sh
        /etc/init.d/elasticsearch start
        /etc/init.d/rabbitmq-server start
        /opt/storm/bin/storm nimbus 2>&1 > /opt/storm/logs/nimbus-console.out &
        /opt/storm/bin/storm supervisor 2>&1 > /opt/storm/logs/supervisor-console.out &

### clone the Lumify projects

        cd ~
        git clone https://github.com/lumifyio/lumify-root.git
        git clone https://github.com/lumifyio/lumify.git


### configure Lumify

        sudo mkdir -p /opt/lumify/{config,lib,logs}

        cd ~/lumify
        sudo cp docs/{lumify.properties,log4j.xml} /opt/lumify/config

        ip_address=$(ip addr show eth0 | awk '/inet / {print $2}' | cut -d / -f 1)
        sudo sed -i -e "s/192.168.33.10/${ip_address}/" /opt/lumify/config/lumify.properties

        sudo /usr/lib/hadoop/bin/hadoop fs -mkdir -p /lumify/libcache


## Build and Deploy Lumify

### install Lumify Root

        cd ~/lumify-root
        export JAVA_HOME=/opt/jdk
        export PATH=$JAVA_HOME/bin:$PATH
        mvn install

### build and deploy the Lumify web application and authentication plugin

        cd ~/lumify
        export JAVA_HOME=/opt/jdk
        export PATH=$JAVA_HOME/bin:$PATH
        mvn package -P web-war -pl lumify-web-war -am
        mvn package -pl lumify-web-auth-username-only -am

        sudo cp lumify-web-war/target/lumify-web-war-0.2.0-SNAPSHOT.war /opt/jetty/webapps/lumify.war
        sudo cp lumify-web-auth-username-only/target/lumify-web-auth-username-only-0.2.0-SNAPSHOT.jar /opt/lumify/lib

        sudo /opt/jetty/bin/jetty.sh restart

1. browse to https://localhost:8443/lumify
1. login by entering any username (if using the `lumify-web-auth-username-only` plugin)


### build and deploy the Lumify Storm topology

        cd ~/lumify
        export JAVA_HOME=/opt/jdk
        export PATH=$JAVA_HOME/bin:$PATH
        mvn package -pl lumify-storm -am

**TODO: build and copy property workers**

        /opt/storm/bin/storm jar lumify-storm/target/lumify-storm-0.2.0-SNAPSHOT-jar-with-dependencies.jar io.lumify.storm.StormRunner

