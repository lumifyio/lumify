
1. Configure has media edge types in your `lumify.properties` file, these IRIs represent the edge labels from an entity to an images. e.g.:

        assignImageMR.hasImageLabel.0=http://lumify.io/palantir-import#hasMedia

1. Run:

        yarn jar lumify-assign-image-mr-*-SNAPSHOT-jar-with-dependencies.jar
