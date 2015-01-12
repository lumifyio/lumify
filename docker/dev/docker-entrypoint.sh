#!/bin/bash
set -e

function shutdown {
  /opt/stop.sh
}

trap 'shutdown' SIGHUP SIGINT SIGTERM

if [ "$1" = 'start' ]; then
  echo "starting"
  /opt/start.sh
else
  "$@"
fi

shutdown
