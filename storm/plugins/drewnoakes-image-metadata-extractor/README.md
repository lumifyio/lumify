# drew-noakes-image-metadata-extractor

This module extracts metadata contained in image files and create Lumify properties with those values. It also detects
if images need to be rotated and/or flipped in order to be displayed in the correct orientation.

The module depends on the following Ontology properties:

        # storm/plugins/drewnoakes-image-metadata-extractor/ontology/exif.owl
        yAxisFlipNeeded
        cwRotationNeeded
        heading

        # examples/ontology-dev/dev.owl
        image
        geoLocation
        dateTaken
        deviceMake
        deviceModel
        width
        height
        fileSize
        metadata

And the following Lumify properties configured:

     ontology.iri.yAxisFlipNeeded=http://lumify.io/exif#yAxisFlipNeeded
     ontology.iri.cwRotationNeeded=http://lumify.io/exif#cwRotationNeeded

     ontology.iri.image=http://lumify.io/dev#image
     ontology.iri.geoLocation=http://lumify.io/dev#geolocation
     ontology.iri.metadata=http://lumify.io/dev#metadata
     ontology.iri.dateTaken=http://lumify.io/dev#dateTaken
     ontology.iri.deviceMake=http://lumify.io/dev#deviceMake
     ontology.iri.deviceModel=http://lumify.io/dev#deviceModel
     ontology.iri.width=http://lumify.io/dev#width
     ontology.iri.height=http://lumify.io/dev#height
     ontology.iri.fileSize=http://lumify.io/dev#fileSize
     ontology.iri.metadata=http://lumify.io/dev#metadata

    # see storm/plugins/drewnoakes-image-metadata-extractor/src/main/java/io/lumify/imageMetadataExtractor/Ontology.java
