
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

## Docker Web Server

1. Create a war file:<br/>
      _NOTE: Run from the host machine in the root of your clone._<br/>
      _NOTE: Requires Oracle JDK._

        mvn package -P web-war -pl web/war -am -DskipTests -Dsource.skip=true

1. Copy the war file:

        cp web/war/target/lumify-web-war*.war \
           docker/lumify-dev-persistent/opt/jetty/webapps/root.war

1. Package an auth plugin:

        mvn package -pl ./web/plugins/auth-username-only -am -DskipTests

1. Copy the auth plugin for use in the docker image:

        cp web/plugins/auth-username-only/target/lumify-web-auth-username-only-*[0-9T].jar \
           docker/lumify-dev-persistent/opt/lumify/lib

1. Inside the docker image run Jetty:

        /opt/jetty/bin/jetty.sh start

1. Open a browser and go to: `http://lumify-dev:8080/`
