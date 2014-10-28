
1. Export your Palantir ontology. You can use the Palantir interface or the Lumify Palantir data importer.
      
      io.lumify.palantir.dataImport.DataImport \
        --namespace=<oracle namespace> \
        --connectionstring=jdbc:oracle:thin:@localhost:1521/ORCL \
        --username=<oracleUsername> \
        --password=<oraclePassword> \
        --owlprefix=http://lumify.io/palantir# \
        --outdir=/palantir/ontology/ \
        --ontologyexport
        
2. Convert the Palantir ontology to an owl file.

      io.lumify.palantir.ontologyToOwl.OntologyToOwl \
        /palantir/ontology/ \
        http://lumify.io/palantir# \
        /palantir/owl/palantir.owl
        
3. Run the data import. The parameters will be the same as step 1 minus the ontologyexport argument.
