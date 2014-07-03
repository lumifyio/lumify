#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

classpath=$(${DIR}/classpath.sh lumify-wikipedia-mr)
if [ $? -ne 0 ]; then
  echo "${classpath}"
  exit
fi

in=${DIR}/../data/ontology/wikipedia.owl
iri=http://lumify.io/wikipedia

java \
-Dfile.encoding=UTF-8 \
-classpath ${classpath} \
-Xmx1024M \
io.lumify.core.cmdline.OwlImport \
--in=${in} \
--iri=${iri}
