#!/bin/sh
#
# rabbitmq RabbitMQ Server
# chkconfig: 2345 84 20
# description: RabbitMQ Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "/opt/rabbitmq/sbin/rabbitmq-plugins --offline enable rabbitmq_management"
    su -s /bin/bash $RUN_AS -c "/opt/rabbitmq/sbin/rabbitmq-server > /dev/null &"
}

stop() {
    su -s /bin/bash $RUN_AS -c "/opt/rabbitmq/sbin/rabbitmqctl stop"
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting RabbitMQ..."
        start
        ;;
    stop)
        echo "Stopping RabbitMQ..."
        stop
        ;;
    restart)
        echo "Restarting RabbitMQ..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/rabbitmq {start|stop|restart}" >&2
        exit 1
        ;;
esac
