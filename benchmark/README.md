This module contains specialized integration tests that measure system performance.

To run benchmarks, use the following command:

```
mvn -Pbenchmark verify
```

You can use an existing test environment by configuring the `testServer` environment variable.

```
mvn -Pbenchmark -DtestServer=lumify-dev verify
```
_Runs with the dev docker container_
