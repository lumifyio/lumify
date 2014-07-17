
Please add the following to the /opt/lumify/lumify.properties file on your local machine:

Add the following to the "# Repository" section.
Note you will need to change the "/Users/jon.hellmann/Documents/lumify-all"
directory to your local lumify directory.:
repository.ontology.owl.3.iri=http://lumify.io/exif
repository.ontology.owl.3.dir=/Users/jon.hellmann/Documents/lumify-all/lumify-public/storm/plugins/drewnoakes-image-metadata-extractor/ontology/

Check and make sure the following are not inside this file:
ontology.iri.yAxisFlipNeeded=http://lumify.io/exif#yAxisFlipNeeded
ontology.iri.cwRotationNeeded=http://lumify.io/exif#cwRotationNeeded


Check that file:
/lumify-all/lumify-public/dev/storm-local/pom.xml
has:
<dependency>
    <groupId>io.lumify</groupId>
    <artifactId>lumify-drewnoakes-image-metadata-extractor</artifactId>
    <version>${project.version}</version>
</dependency>


Check that file:
/lumify-all/lumify-public/storm/plugins/pom.xml
has:
<module>drewnoakes-image-metadata-extractor</module>