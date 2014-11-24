
1. You need to configure the following:

      ontology.iri.hasMedia=http://lumify.io/palantir-import#hasMedia

2. Setup Oracle JDBC jar in your classpath.

3. Setup Oracle Spatial in your classpath. The spacial jar can be found
   in your Oracle installation: ```/u01/app/oracle/product/11.2.0/dbhome_2/md/jlib/sdoapi.jar```
   and ```/u01/app/oracle/product/11.2.0/dbhome_2/md/jlib/sdoutl.jar```

4. Export your Palantir ontology. You can use the Palantir interface or the Lumify Palantir data importer.
      
      io.lumify.palantir.dataImport.DataImport \
        --namespace=<oracle namespace> \
        --connectionstring=jdbc:oracle:thin:@localhost:1521/ORCL \
        --username=<oracleUsername> \
        --password=<oraclePassword> \
        --owlprefix=http://lumify.io/palantir# \
        --outdir=/palantir/ontology/ \
        --ontologyexport
        
5. Convert the Palantir ontology to an owl file.

      io.lumify.palantir.ontologyToOwl.OntologyToOwl \
        /palantir/ontology/ \
        http://lumify.io/palantir \
        /palantir/owl/palantir.owl

6. Import ```/palantir/owl/palantir.owl```

7. Modify ```ontology/palantir-import.owl``` as needed and import that owl file.

8. Run the data import. The parameters will be the same as step 4 minus the ontologyexport argument.
