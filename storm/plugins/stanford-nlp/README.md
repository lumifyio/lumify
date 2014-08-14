


The stanford-nlp module requires the following ontology properties located in the following files:
    File: /lumify/examples/ontology-dev/dev.owl:
        Property: sentiment
     
Please make sure the following are in your /opt/lumify/lumify.properties file:
     # Ontology
     ontology.iri.sentiment=http://lumify.io/dev#sentiment


     
     
Or to create your own ontologies, edit:
    lumify.properties file.
    SENTIMENT_IRI variable in SentimentAnalysisGraphPropertyWorker.java
 
 
