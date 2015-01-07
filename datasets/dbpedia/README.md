## DBpedia Import via Map Reduce

1. Build the jar:

        mvn clean package

1. Download the DBPedia types file (en-nt) from http://wiki.dbpedia.org/Downloads39.
1. Download the DBPedia mapping based properties file (en-nt) from http://wiki.dbpedia.org/Downloads39.
1. Copy the MR input file to HDFS:

        hadoop fs -mkdir -p /lumify
        hadoop fs -put dbpedia-types /lumify
        hadoop fs -put dbpedia-mapping-properties /lumify

1. Submit the MR job:

        hadoop jar lumify-dbpedia-*-jar-with-dependencies.jar /lumify/dbpedia-types
        hadoop jar lumify-dbpedia-*-jar-with-dependencies.jar /lumify/dbpedia-mapping-properties

1. Wait for the MR job to complete

1. [Re-index the data](https://github.com/lumifyio/lumify/tree/master/tools/reindex-mr)

## Running inside an IDE

1. Run format

1. Import the wikipedia.owl

1. Import the dbpedia.owl downloaded from DBpedia's web site.

1. Import the ontology/dbpedia.owl

1. Run `io.lumify.dbpedia.mapreduce.ImportMR dbpedia-types`
