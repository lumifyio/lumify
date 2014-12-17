
The dev docker image contains all the backend services needed for development. To get started run:

```
./build-dev.sh
```

This will build an image by downloading and installing all the necessary components into the docker image. After
this completes run:

```
./run-dev.sh
```

This will start the docker image and leave you in a bash shell within the image. This will also map all the internal
ports to external ports so that you can run the web server against the services.

There are helper scripts within the image `/opt/start.sh` and `/opt/stop.sh` to start and stop all the services.
