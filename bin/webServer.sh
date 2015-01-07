#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

classpath=$(${DIR}/classpath.sh dev/jetty-server)
if [ $? -ne 0 ]; then
  echo "${classpath}"
  exit
fi

[ "${DEBUG_PORT}" ] || DEBUG_PORT=12345
[ "$1" = '-d' ] && debug_option="-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=${DEBUG_PORT}"

cd ${DIR}/../

java ${debug_option} \
-Dfile.encoding=UTF-8 \
-Djava.awt.headless=true \
-Djava.security.krb5.realm= \
-Djava.security.krb5.kdc= \
-Dlumify.request.debug=true \
-classpath ${classpath} \
-Xmx1024M \
io.lumify.web.JettyWebServer \
--port=8080 \
--httpsPort=8443 \
--keyStorePath=${DIR}/../dev/jetty-server/config/lumify-vm.lumify.io.jks \
--keyStorePassword=password \
--webAppDir=${DIR}/../web/war/src/main/webapp
