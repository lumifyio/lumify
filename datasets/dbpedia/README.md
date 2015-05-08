## DBpedia Import via Map Reduce

1. Build the jar:

        mvn clean package
1. Import ontologies using web interface. Ontologoies needed for this job are wikipedia and dbpedia. Zip the ontology files (.owl) and any associated files such as images. Using Admin Upload Ontoloty, upload the zip file and make sure the ontologies are succesfully loaded before proceeding to the next step
        
1. If you are using Docker in your development environment, make sure your docker container has at least 60GB space and more than 8GB memory. If that's not the case, you need to reintialize your docker image and rebuild it. as below:

		boot2docker init -m 12000 -s 61440

1. After that you need to start docker container with run-dev.sh. If you are not using docker, login to one of the hadoop data nodes.
1. If you are using docker, create directories datasets/dbpedia/dbpedia-types and datasets/dbpedia/dbpedia-mapping-properties in your workstation under docker/lumify-dev-persistent directory. This should create directories in your docker image as below:
/opt/lumify/datasets/dbpedia 
/opt/lumify/datasets/dbpedia/dbpedia-types
/opt/lumify/datasets/dbpedia/dbpedia-mapping-properties

1. Download the DBPedia types file (en-nt) from http://wiki.dbpedia.org/Downloads39 to the direcrtory: /opt/lumify/datasets/dbpedia/dbpedia-types

1. Download the DBPedia mapping based properties file (en-nt) from http://wiki.dbpedia.org/Downloads39 to the directory: /opt/lumify/datasets/dbpedia/dbpedia-mapping-properties
1. If the downloads are not succesful, sample datafiles and types are checked into git repostitory under /dbpedia-types and /dbpedia-mapping-properties project. You can copy the files from there to the above mentioned directories 

1. cd /opt/lumify/datasets/dbpedia/
1. Create lumify directory in Hadoop
            
         hadoop fs -mkdir -p /lumify
1. Copy the MR input files to HDFS:
        
        hadoop fs -put dbpedia-types /lumify
        hadoop fs -put dbpedia-mapping-properties /lumify

1. Copy the map reduce job jar (lumify-dbpedia-*-jar-with-dependencies.jar) to /opt/lumify/lib folder.a
1. cd /opt/lumify/lib 
1. Submit the MR job:

        hadoop jar lumify-dbpedia-*-jar-with-dependencies.jar /lumify/dbpedia-types
        hadoop jar lumify-dbpedia-*-jar-with-dependencies.jar /lumify/dbpedia-mapping-properties

1. Wait for the MR job to complete

1. [Re-index the data](https://github.com/lumifyio/lumify/tree/master/tools/reindex-mr)

## Running inside an IDE

1. Run format

1. Import ontologies using web interface. Ontologoies needed for this job are wikipedia and dbpedia. Zip the ontology files (.owl) and any associated files such as images. Using Admin Upload Ontoloty, upload the zip file and make sure the ontologies are succesfully loaded before proceeding to the next step

1. Run `io.lumify.dbpedia.mapreduce.ImportMR dbpedia-types`
