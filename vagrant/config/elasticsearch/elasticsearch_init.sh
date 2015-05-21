#!/bin/sh
#
# elasticsearch Elasticsearch Server
# chkconfig: 2345 83 21
# description: Elasticsearch Service

export JAVA_HOME=/opt/jdk
export JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8

RUN_AS=root

start() {
    su -s /bin/bash $RUN_AS -c "mkdir -p /var/log/elasticsearch"
    su -s /bin/bash $RUN_AS -c "/opt/elasticsearch/bin/elasticsearch > /dev/null &"
}

stop() {
    curl -XPOST 'http://localhost:9200/_cluster/nodes/_local/_shutdown'
    echo ""
}

restart() {
    stop
    sleep 3
    start
}

case "$1" in
    start)
        echo "Starting Elasticsearch..."
        start
        ;;
    stop)
        echo "Stopping Elasticsearch..."
        stop
        ;;
    restart)
        echo "Restarting Elasticsearch..."
        restart
        ;;
    *)
        echo "Usage: /etc/init.d/elasticsearch {start|stop|restart}" >&2
        exit 1
        ;;
esac
