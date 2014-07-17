Please add the following to the /opt/lumify/lumify.properties file on your local machine:

Add the following to the "# Ontology" section:
ontology.iri.yAxisFlipNeeded=http://lumify.io/exif#yAxisFlipNeeded
ontology.iri.cwRotationNeeded=http://lumify.io/exif#cwRotationNeeded

Add the following to the "# Repository" section:
Note you will need to change the "/Users/jon.hellmann/Documents/lumify-all"
directory to your local lumify directory.:
repository.ontology.owl.3.iri=http://lumify.io/exif
repository.ontology.owl.3.dir=/Users/jon.hellmann/Documents/lumify-all/lumify-public/storm/plugins/drewnoakes-image-metadata-extractor/ontology/
