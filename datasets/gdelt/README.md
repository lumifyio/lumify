## GDELT Import via Map Reduce

1. Build the jar:

        mvn clean package

1. Copy the MR input file to HDFS:

        hadoop fs -mkdir -p /lumify/gdelt
        hadoop fs -put data/20140701.export.txt /lumify/gdelt

1. Submit the MR job:

        hadoop jar lumify-gdelt-mr-*-jar-with-dependencies.jar /lumify/gdelt/20140701.export.txt

1. Wait for the MR job to complete

## Running inside an IDE

1. Run format

1. Import the dev.owl

1. Import the gdelt.owl

1. Run `io.lumify.gdelt.GDELTRunner data/20140701.export.txt`

1. Run the public storm topology.
