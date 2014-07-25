#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
BIN_DIR=${DIR}/../../../bin

classpath=$(${BIN_DIR}/classpath.sh datasets/gdelt/lumify-gdelt-mr)
if [ $? -ne 0 ]; then
  echo "${classpath}"
  exit
fi

java \
-Xmx512m \
-Dfile.encoding=UTF-8 \
-Djava.library.path=$LD_LIBRARY_PATH \
-classpath ${classpath} \
io.lumify.gdelt.GDELTRunner \
$*
