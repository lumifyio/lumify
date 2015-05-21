#!/bin/sh
#
# hadoop
# chkconfig: 2345 81 23
# description: Hadoop Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
export HADOOP_PREFIX=/opt/hadoop
export HADOOP_HDFS_HOME=/opt/hadoop
export HADOOP_COMMON_HOME=/opt/hadoop
export HADOOP_YARN_HOME=/opt/hadoop
export HADOOP_CONF_DIR=/opt/hadoop/etc/hadoop
export YARN_CONF_DIR=/opt/hadoop/etc/hadoop
export HADOOP_MAPRED_HOME=/opt/hadoop


RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "sed s/HOSTNAME/$HOSTNAME/ /opt/hadoop/etc/hadoop/core-site.xml.template > /opt/hadoop/etc/hadoop/core-site.xml"
    su -s /bin/bash $RUN_AS -c "mkdir -p /var/log/hadoop"

    if [ ! -d "/tmp/hadoop-root" ]; then
        echo "**************** FORMATING NAMENODE ****************"
        su -s /bin/bash $RUN_AS -c "/opt/hadoop/bin/hdfs namenode -format"
    fi
    su -s /bin/bash $RUN_AS -c "/opt/hadoop/sbin/start-dfs.sh"
    su -s /bin/bash $RUN_AS -c "/opt/hadoop/sbin/start-yarn.sh"
    su -s /bin/bash $RUN_AS -c "/opt/hadoop/bin/hdfs dfsadmin -safemode wait"
}

stop() {
    su -s /bin/bash $RUN_AS -c "/opt/hadoop/sbin/stop-yarn.sh"
    su -s /bin/bash $RUN_AS -c "/opt/hadoop/sbin/stop-dfs.sh"
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting Hadoop..."
        start
        ;;
    stop)
        echo "Stopping Hadoop..."
        stop
        ;;
    restart)
        echo "Restarting Hadoop..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/hadoop {start|stop|restart}" >&2
        exit 1
        ;;
esac
