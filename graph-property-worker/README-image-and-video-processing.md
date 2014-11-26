
# Image and Video Processing

1. Copy `graph-property-worker-base/ontology/media.owl` and edit the `<!ENTITY>` definitions to suit your exiting ontology

1. Import your custom media ontology

1. Configure the following Lumify properties:

        ontology.iri.media.yAxisFlipped=http://lumify.io/media#yAxisFlipped
        ontology.iri.media.clockwiseRotation=http://lumify.io/media#clockwiseRotation
        ontology.iri.media.heading=http://lumify.io/media#imageHeading
        ontology.iri.media.dateTaken=http://lumify.io/media#dateTaken
        ontology.iri.media.deviceMake=http://lumify.io/media#deviceMake
        ontology.iri.media.deviceModel=http://lumify.io/media#deviceModel
        ontology.iri.media.width=http://lumify.io/media#width
        ontology.iri.media.height=http://lumify.io/media#height
        ontology.iri.media.fileSize=http://lumify.io/media#fileSize
        ontology.iri.media.metadata=http://lumify.io/media#metadata

        # as defined in your existing ontology
        ontology.iri.media.geoLocation=http://lumify.io/dev#geolocation
        ontology.iri.media.duration=http://lumify.io/dev#duration
