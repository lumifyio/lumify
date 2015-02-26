
1. Configure has media edge types in your lumify.properties. These IRIs represent the edge labels from an entity to an images:

      assignImageMR.hasImageLabel.0=<edge iri>
      assignImageMR.hasImageLabel.1=<edge iri>

1. Run:

        yarn jar lumify-assign-image-mr-*-SNAPSHOT-jar-with-dependencies.jar
