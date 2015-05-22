#!/bin/sh
#
# accumulo Accumulo Server
# chkconfig: 2345 82 22
# description: Accumulo Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "echo $HOSTNAME > /opt/accumulo/conf/masters"
    su -s /bin/bash $RUN_AS -c "echo $HOSTNAME > /opt/accumulo/conf/slaves"
    su -s /bin/bash $RUN_AS -c "echo $HOSTNAME > /opt/accumulo/conf/tracers"
    su -s /bin/bash $RUN_AS -c "echo $HOSTNAME > /opt/accumulo/conf/gc"
    su -s /bin/bash $RUN_AS -c "echo $HOSTNAME > /opt/accumulo/conf/monitor"
    su -s /bin/bash $RUN_AS -c "mkdir -p /var/log/accumulo"

    if [ $(su -s /bin/bash $RUN_AS -c "/opt/hadoop/bin/hadoop fs -ls /user | grep accumulo | wc -l") == "0" ]; then
        echo "Creating accumulo user in hdfs"
        su -s /bin/bash $RUN_AS -c "/opt/hadoop/bin/hadoop fs -mkdir -p /user/accumulo"
        su -s /bin/bash $RUN_AS -c "/opt/hadoop/bin/hadoop fs -chown accumulo /user/accumulo"
    fi

    if su -s /bin/bash $RUN_AS -c "/opt/accumulo/bin/accumulo info" 2>&1 | grep --quiet "Accumulo not initialized"; then
        echo "**************** INITIALIZING ACCUMULO ****************"
        su -s /bin/bash $RUN_AS -c "/opt/accumulo/bin/accumulo init --instance-name lumify --password password --clear-instance-name"
    fi
    su -s /bin/bash $RUN_AS -c "/opt/accumulo/bin/start-all.sh"
}

stop() {
    su -s /bin/bash $RUN_AS -c "/opt/accumulo/bin/stop-all.sh"
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting Accumulo..."
        start
        ;;
    stop)
        echo "Stopping Accumulo..."
        stop
        ;;
    restart)
        echo "Restarting Accumulo..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/accumulo {start|stop|restart}" >&2
        exit 1
        ;;
esac
