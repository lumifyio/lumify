#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
cd ${DIR}

unamestr=`uname`

if [[ "$unamestr" == 'Linux' ]]; then
   sudo docker build -t lumifyio/dev dev
elif [[ "$unamestr" == 'Darwin' ]]; then
   docker build -t lumifyio/dev dev
fi
