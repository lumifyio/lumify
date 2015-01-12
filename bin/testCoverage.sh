#!/bin/bash
#
# This script provides an accurate code coverage report using Cobertura. Integration tests are included.
# Based on http://thomassundberg.wordpress.com/2012/02/18/test-coverage-in-a-multi-module-maven-project/
#
# This must be run from the root project directory:
#   bin/testCoverage.sh
# If you want to use your local server so the integration tests run faster, define the ITEST_SERVER env variable:
#   ITEST_SERVER=lumify-dev bin/testCoverage.sh
# Note that using ITEST_SERVER wipes all data from the test server.
#

trap 'exit' ERR

COBERTURA_TARGET_DIR=target/cobertura
TEMP_SRC_DIR=${COBERTURA_TARGET_DIR}/src
TEST_CLASSPATH_FILE=${COBERTURA_TARGET_DIR}/classpath
REPORT_DIR=${COBERTURA_TARGET_DIR}/report
COBERTURA_SER_FILE=${COBERTURA_TARGET_DIR}/cobertura.ser

# Instrument
mvn clean compile cobertura:instrument
find . -name generated-classes | while read dir
do
  cp -R ${dir}/cobertura/* $(dirname ${dir})/classes/
  rm -r ${dir}/cobertura
done

#Test
if [ -z "${ITEST_SERVER}" ]; then
  mvn -PITest -Drepository.ontology=io.lumify.core.model.ontology.ReadOnlyInMemoryOntologyRepository test
else
  mvn -PITest -Drepository.ontology=io.lumify.core.model.ontology.ReadOnlyInMemoryOntologyRepository -DtestServer=${ITEST_SERVER} test
fi

# Merge
mkdir -p ${TEMP_SRC_DIR}
find . -name "main" | grep "/src/" | while read dir
do
  cp -R ${dir}/java/ ${TEMP_SRC_DIR}
done
mvn -pl . dependency:build-classpath -DincludeScope=test -Dmdep.outputFile=${TEST_CLASSPATH_FILE}
find . -name "*.ser" | xargs java -cp $(cat ${TEST_CLASSPATH_FILE}) net.sourceforge.cobertura.merge.Main \
  --datafile ${COBERTURA_SER_FILE}

# Report
java -cp $(cat ${TEST_CLASSPATH_FILE}) net.sourceforge.cobertura.reporting.Main \
  --datafile ${COBERTURA_SER_FILE} --destination ${REPORT_DIR} --format html ${TEMP_SRC_DIR}
java -cp $(cat ${TEST_CLASSPATH_FILE}) net.sourceforge.cobertura.reporting.Main \
  --datafile ${COBERTURA_SER_FILE} --destination ${REPORT_DIR} --format xml ${TEMP_SRC_DIR}
