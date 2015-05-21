#!/bin/sh
#
# jetty Jetty Server
# chkconfig: 2345 85 19
# description: Jetty Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
export JETTY_HOME=/opt/jetty
export PATH=$PATH:/opt/jdk/bin

RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "/opt/jetty/bin/jetty.sh start"
}

stop() {
    su -s /bin/bash $RUN_AS -c "/opt/jetty/bin/jetty.sh stop"
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting Jetty..."
        start
        ;;
    stop)
        echo "Stopping Jetty..."
        stop
        ;;
    restart)
        echo "Restarting Jetty..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/jetty {start|stop|restart}" >&2
        exit 1
        ;;
esac