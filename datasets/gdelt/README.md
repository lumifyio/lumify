## GDELT Import via Map Reduce

All commands listed in the steps below are assumed to be run from the root directory of the lumify project.

1. import the GDELT ontology:

        bin/owlImport.sh -i datasets/gdelt/ontology/gdelt.owl --iri http://lumify.io/gdelt

1. copy the GDELT data file(s) to HDFS:

        hadoop fs -mkdir -p /lumify/gdelt
        hadoop fs -put datasets/gdelt/data/small.export.txt /lumify/gdelt

1. build the GDELT MR and re-index MR jars:

        mvn package -pl datasets/gdelt/lumify-gdelt-mr -am
        mvn package -pl tools/reindex-mr -am

1. submit the GDELT MR job:

        hadoop jar datasets/gdelt/lumify-gdelt-mr/target/lumify-gdelt-mr-*-jar-with-dependencies.jar /lumify/gdelt

1. wait for the GDELT MR job to complete

1. submit the re-index MR job:

        hadoop jar tools/reindex-mr/target/lumify-reindex-mr-*-jar-with-dependencies.jar vertex

1. search results will be incrementally available as the re-index MR job runs
