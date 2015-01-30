# Lumify

![Lumify Logo](web/war/src/main/webapp/img/lumify-logo.png?raw=true)

Lumify is an open source big data analysis and visualization platform. Please see [http://lumify.io](http://lumify.io) for more details and videos.

## Getting Started

To get started quickly, you can try out a hosted installation of Lumify, or download a virtual machine image with Lumify installed and pre-configured.

- [Try Lumify Now](http://lumify.io/try.html)
- [Watch Lumify Videos](https://www.youtube.com/playlist?list=PLDX7b-6_sNA7SCJw5rB9EF0TDpQyrO2XR)

## Quick Start

1. Install Docker per their instructions: [https://docs.docker.com/installation](https://docs.docker.com/installation/#installation)

1. Install node and npm per their instructions: [http://nodejs.org/](http://nodejs.org/)

1. Install the Lumify npm dependencies (from the web/war/src/main/webapp directory):

        npm install -g inherits bower grunt
        npm install grunt-cli

1. Update your hosts file:
    - Linux

            echo '127.0.0.1 lumify-dev' >> /etc/hosts

    - OS X

            echo "$(boot2docker ip 2>/dev/null) lumify-dev" >> /etc/hosts

1. Create the docker image:

        docker/build-dev.sh

1. Run the docker image: (This will start ZooKeeper, HDFS, YARN, ElasticSearch, and RabbitMQ)

        docker/run-dev.sh

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

See [docker/README.md](docker/) for more information on the docker dev image.

See [docs/developer.md](docs/developer.md) for more information on developing for Lumify.

## Developing / Contributing

If you're a system administrator or developer looking to install your own instance of Lumify or do custom development,
please read our [Developer Guide](docs/developer.md).


## License

Copyright 2014 Altamira Technologies Corporation

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
