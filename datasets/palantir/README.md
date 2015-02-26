1. Build the jars (from the root of your clone):

        mvn package -am -pl datasets/palantir/data-to-sequence-file,datasets/palantir/import-mr

1. Setup Oracle JDBC jar in your classpath.

1. Setup Oracle Spatial in your classpath. The spacial jar files can be found in your Oracle installation:

        /u01/app/oracle/product/11.2.0/dbhome_2/md/jlib/sdoapi.jar
        /u01/app/oracle/product/11.2.0/dbhome_2/md/jlib/sdoutl.jar

1. Export your Palantir data to sequence files:

        java \
          -cp sdoapi.jar:sdoutl.jar:ojdbc6.jar:lumify-palantir-data-to-sequence-file-*-SNAPSHOT-with-dependencies.jar \
          io.lumify.palantir.DataToSequenceFile \
          --namespace=<oracle namespace> \
          --connectionstring=jdbc:oracle:thin:@localhost:1521/ORCL \
          --username=<oracleUsername> \
          --password=<oraclePassword> \
          --dest=hdfs:///palantir-export/ \
          --baseiri=http://lumify.io/palantir#

1. Import `examples/ontology-minimal/minimal.owl`

1. Import `datasets/palantir/ontology/palantir-import.owl`

1. Import `hdfs://palantir-export/owl/palantir.owl`

1. Create an import MR jar with the Oracle dependencies

        mkdir palantir-import-mr
        (cd palantir-import-mr; jar -xf ../lumify-palantir-import-mr-*-SNAPSHOT-with-dependencies.jar)
        (cd palantir-import-mr; jar -xf ../sdoapi.jar)
        (cd palantir-import-mr; jar -xf ../sdoutl.jar)
        (cd palantir-import-mr; jar -xf ../ojdbc6.jar)
        jar -cf palantir-import-mr.jar -C palantir-import-mr .

1. Run the import MR process `io.lumify.palantir.mr.ImportMR` as the mapred user.

        yarn jar palantir-import-mr.jar io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtUser http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtGraph http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtObject http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtGraphObject http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtObjectObject http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtMediaAndValue http://lumify.io/palantir#
        io.lumify.palantir.mr.ImportMR hdfs:///palantir-export/ PtPropertyAndValue http://lumify.io/palantir#

1. Reindex MR

1. Requeue raw
