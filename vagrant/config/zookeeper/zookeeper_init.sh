#!/bin/sh
#
# zookeeper ZooKeeper Server
# chkconfig: 2345 80 24
# description: ZooKeeper Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
export ZOOKEEPER_HOME=/opt/zookeeper

RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "/opt/zookeeper/bin/zkServer.sh start"
}

stop() {
    su -s /bin/bash $RUN_AS -c "/opt/zookeeper/bin/zkServer.sh stop"
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting ZooKeeper..."
        start
        ;;
    stop)
        echo "Stopping ZooKeeper..."
        stop
        ;;
    restart)
        echo "Restarting ZooKeeper..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/zookeeper {start|stop|restart}" >&2
        exit 1
        ;;
esac

