## Running inside an IDE

1. Have lumify set up via [instructions](../../docs/developer.md).

1. Have the dev.owl imported via [instructions](../../docs/ontology.md).

1. Compress wikipedia owl folder and import the same way as the dev.owl
        
        <lumify working dir>/datasets/wikipedia/data/ontology

1. Run within your IDE

        Main class:                 io.lumify.wikipedia.mapreduce.ImportMR
        Program arguments:          enwiki-20140102-pages-articles-lines-10.xml
        Working directory:          <lumify working dir>/datasets/wikipedia/data
        Use classpath of module:    lumify-wikipedia-mr

## Wikipedia Import via Map Reduce

1. Build the jar:

        mvn clean package

1. Get Wikipedia XML data using one of the following:

   * Download the whole dataset from http://meta.wikimedia.org/wiki/Data_dump_torrents#enwiki
   * Use the smaller [sample dataset](data/enwiki-20140102-pages-articles-lines-10.xml) in the data directory.

1. Convert the well-formed XML to one XML page element per line:

        java -cp umify-wikipedia-mr-*-jar-with-dependencies.jar \
          io.lumify.wikipedia.mapreduce.WikipediaFileToMRFile \
          -in enwiki-20140102-pages-articles.xml
          -out enwiki-20140102-pages-articles.MR.txt

1. Copy the MR input file to HDFS:

        hadoop fs -mkdir -p /lumify
        hadoop fs -put enwiki-20140102-pages-articles.MR.txt /lumify

1. Pre-split destination Accumulo tables:

        bin/configure_splits.sh

1. Submit the MR job:

        hadoop jar lumify-wikipedia-mr-*-jar-with-dependencies.jar /lumify/enwiki-20140102-pages-articles.MR.txt

1. Wait for the MR job to complete

1. [Re-index the data](https://github.com/lumifyio/lumify/tree/master/tools/reindex-mr)
