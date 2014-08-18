
# Image and Video Processing

The video processing aspects of this module requires the following ontology properties located in the following files:
    File: /lumify/examples/ontology-dev/dev.owl:
        Property: video
        Property: image
        Property: videoDuration
        Property: videoRotation
        Property: geoLocation
        Property: lastModifyDate
        Property: dateTaken
        Property: deviceMake
        Property: deviceModel
        Property: width
        Property: height
        Property: fileSize
        Property: metadata

Please make sure the following are in your /opt/lumify/lumify.properties file:
     # Ontology
     ontology.iri.image=http://lumify.io/dev#image
     ontology.iri.geoLocation=http://lumify.io/dev#geolocation
     ontology.iri.metadata=http://lumify.io/dev#metadata
     ontology.iri.dateTaken=http://lumify.io/dev#dateTaken
     ontology.iri.deviceMake=http://lumify.io/dev#deviceMake
     ontology.iri.deviceModel=http://lumify.io/dev#deviceModel
     ontology.iri.width=http://lumify.io/dev#width
     ontology.iri.height=http://lumify.io/dev#height
     ontology.iri.fileSize=http://lumify.io/dev#fileSize

Or to create your own ontologies, edit the file:
 /lumify/storm/plugins/drewnoakes-image-metadata-extractor/src/main/java/io/lumify/imageMetadataExtractor/Ontology.java
 and the lumify.properties file.
 