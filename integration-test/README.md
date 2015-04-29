To run integration tests, use the command:

```
mvn -PITest verify
```

You can run only integration tests in a full build with the command:

```
mvn -PITest -DskipTests=true verify
```

You can use the dev docker container as the Lumify environment and the in-memory Ontology repository
to speed up test execution.

```
mvn -PITest -DtestServer=lumify-dev -Drepository.ontology=io.lumify.core.model.ontology.InMemoryOntologyRepository verify
```
