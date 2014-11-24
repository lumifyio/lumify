
# Running with Live Data

1. Add a file to your lumify configuration directory. (eg ```/opt/lumify/config/flight-aware.properties```)

```
flightaware.username=<your flightaware username>
flightaware.apikey=<your flightaware apikey>
```

2. Run maven to create a FlightAware jar with dependencies

```
mvn -am -pl datasets/flight-aware/ -DskipTests package
```

3. Run

```
java -jar datasets/flight-aware/target/lumify-flight-aware-*-with-dependencies.jar \
   io.lumify.flightTrack.FlightAware
   --query="-idents VRD*"
   --out=/tmp/flightaware
```

# Running Replay

1. Run maven to create a FlightAware jar with dependencies

```
mvn -am -pl datasets/flight-aware/ -DskipTests package
```

2. Run

```
java -cp datasets/flight-aware/target/lumify-flight-aware-*-with-dependencies.jar \
  io.lumify.flightTrack.Replay
  --in=datasets/flight-aware/sample-data
```
