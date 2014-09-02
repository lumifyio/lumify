# drew-noakes-image-metadata-extractor

This module extracts metadata contained in image files and create Lumify properties with those values. It also detects
if images need to be rotated and/or flipped in order to be displayed in the correct orientation.

## Ontology

The `storm/plugins/drewnoakes-image-metadata-extractor/ontology/exif.owl` file defines the following properties:

        http://lumify.io/exif#yAxisFlipNeeded
        http://lumify.io/exif#cwRotationNeeded
        http://lumify.io/exif#heading
        http://lumify.io/exif#dateTaken
        http://lumify.io/exif#deviceMake
        http://lumify.io/exif#deviceModel
        http://lumify.io/exif#width
        http://lumify.io/exif#height
        http://lumify.io/exif#fileSize
        http://lumify.io/exif#metadata

It adds these properties as well as `http://lumify.io/dev#geolocation` to the `http://lumify.io/dev#image` and `http://lumify.io/dev#video` concepts.


## Properties

The following Lumify properties must be configured:

     ontology.iri.yAxisFlipNeeded=http://lumify.io/exif#yAxisFlipNeeded
     ontology.iri.cwRotationNeeded=http://lumify.io/exif#cwRotationNeeded
     ontology.iri.heading=http://lumify.io/exif#heading
     ontology.iri.dateTaken=http://lumify.io/exif#dateTaken
     ontology.iri.deviceMake=http://lumify.io/exif#deviceMake
     ontology.iri.deviceModel=http://lumify.io/exif#deviceModel
     ontology.iri.width=http://lumify.io/exif#width
     ontology.iri.height=http://lumify.io/exif#height
     ontology.iri.fileSize=http://lumify.io/exif#fileSize
     ontology.iri.metadata=http://lumify.io/exif#metadata

     ontology.iri.image=http://lumify.io/dev#image
     ontology.iri.geoLocation=http://lumify.io/dev#geolocation

    # see storm/plugins/drewnoakes-image-metadata-extractor/src/main/java/io/lumify/imageMetadataExtractor/Ontology.java
