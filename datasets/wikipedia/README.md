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

## Running inside an IDE

1. Run format

1. [Import the dev.owl](../../docs/ontology.md)

1. [Import the wikipedia.owl](../../docs/ontology.md)

1. Run `io.lumify.wikipedia.mapreduce.ImportMR enwiki-20140102-pages-articles-lines-10.xml`

1. [Re-index the data](../../tools/reindex-mr)
