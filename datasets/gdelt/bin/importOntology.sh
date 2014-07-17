#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
BIN_DIR=${DIR}/../../../bin

classpath=$(${BIN_DIR}/classpath.sh tools/cli)
if [ $? -ne 0 ]; then
  echo "${classpath}"
  exit
fi

in=${DIR}/../data/ontology/gdelt.owl
iri=http://lumify.io/gdelt

java \
-Dfile.encoding=UTF-8 \
-classpath ${classpath} \
-Xmx1024M \
io.lumify.core.cmdline.OwlImport \
--in=${in} \
--iri=${iri}
