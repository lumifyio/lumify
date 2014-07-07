#!/bin/bash

SOURCE="${BASH_SOURCE[0]}"
while [ -h "$SOURCE" ]; do
  DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"
  SOURCE="$(readlink "$SOURCE")"
  [[ $SOURCE != /* ]] && SOURCE="$DIR/$SOURCE"
done
DIR="$(cd -P "$(dirname "$SOURCE")" && pwd)"

dir=$1
filename=${DIR}/../${dir}/target/.classpath.$(id -un)

if [ -d ${DIR}/../${dir} ]; then
  if [ "${RUN_MVN}" != '' ]; then
    run_mvn='true'
  elif [ ! -f ${filename} ]; then
    run_mvn='true'
  else
    for pom in $(find ${DIR}/.. -name 'pom.xml'); do
      if [ ${pom} -nt ${filename} ]; then
        run_mvn='true'
        break
      fi
    done
  fi

  if [ "${run_mvn}" == 'true' ]; then
    echo 'running maven to calculate the classpath...' >&2
    [ "${MVN_OPTS}" ] && echo "MVN_OPTS is ${MVN_OPTS}" >&2
    mvn_output="$(cd ${DIR}/.. && mvn clean compile -DskipTests ${MVN_OPTS})"
    mvn_exit=$?
    if [ ${mvn_exit} -ne 0 ]; then
      echo "${mvn_output}"
      exit ${mvn_exit}
    fi
    echo 'maven finished.' >&2
  else
    echo 'not running maven, using cached classpath.' >&2
  fi

  if [ -f ${filename} ]; then
    if [ -d ${DIR}/../${dir}/target/classes ]; then
      echo "${DIR}/../${dir}/target/classes:$(cat ${filename})"
    else
      echo "${dir}/target/classes not found"
      exit 3
    fi
  else
    echo "${filename} not found"
    exit 2
  fi
else
  echo "${dir} is not a valid directory"
  exit 1
fi
