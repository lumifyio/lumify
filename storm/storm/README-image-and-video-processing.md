
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


 private static final String AUDIO_DURATION_IRI = "ontology.iri.audioDuration";
     private static final String VIDEO_DURATION_IRI = "ontology.iri.videoDuration";
     private static final String VIDEO_ROTATION_IRI = "ontology.iri.videoRotation";
     private static final String CONFIG_GEO_LOCATION_IRI = "ontology.iri.geoLocation";
     private static final String LAST_MODIFY_DATE_IRI = "ontology.iri.lastModifyDate";
     private static final String DATE_TAKEN_IRI = "ontology.iri.dateTaken";
     private static final String DEVICE_MAKE_IRI = "ontology.iri.deviceMake";
     private static final String DEVICE_MODEL_IRI = "ontology.iri.deviceModel";
     private static final String METADATA_IRI = "ontology.iri.metadata";
     private static final String WIDTH_IRI = "ontology.iri.width";
     private static final String HEIGHT_IRI = "ontology.iri.height";
     private static final String FILE_SIZE_IRI = "ontology.iri.fileSize";