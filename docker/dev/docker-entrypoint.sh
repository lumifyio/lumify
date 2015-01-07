#!/bin/bash
set -e

if [ "$1" = 'start' ]; then
  echo "starting"
  /opt/start.sh
fi

exec "$@"
