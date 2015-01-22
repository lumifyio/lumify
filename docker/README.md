
## Running

The dev docker image contains all the backend services needed for development. To get started run:

        ./build-dev.sh

This will build an image by downloading and installing all the necessary components into the docker image. After
this completes run:

        ./run-dev.sh

This will start the docker image and leave you in a bash shell within the image. This will also map all the internal
ports to external ports so that you can run the web server against the services.

There are helper scripts within the image `/opt/start.sh` and `/opt/stop.sh` to start and stop all the services.

It is also helpful to add the following to your `/etc/hosts` file:

        127.0.0.1       lumify-dev

## Formatting

To format your dev image, you can run the format script.

        ./format-dev.sh
