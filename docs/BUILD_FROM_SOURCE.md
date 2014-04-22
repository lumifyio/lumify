# Building from Source

## Build Dependencies

Lumify has several required dependencies to build from source. Please ensure that the following projects are installed before building:
* [Java 1.6](http://www.oracle.com/technetwork/java/javasebusiness/downloads/java-archive-downloads-javase6-419409.html)
* [Maven 3.1.1](http://maven.apache.org/download.cgi)
* [Jetty 8.1.14.v20131031](http://download.eclipse.org/jetty/stable-8/dist/)
* [CDH 4.4](http://www.cloudera.com/content/support/en/downloads/download-components/download-products.html)
* [Accumulo 1.5](http://accumulo.apache.org/downloads/)
* [Elastic Search 0.90.0](http://www.elasticsearch.org/downloads/0-90-0/)
* [Storm 0.8.1](http://storm-project.net/downloads.html)
* [RabbitMQ 3.3.0](http://www.rabbitmq.com/download.html)
* [NodeJS 0.10.21](http://blog.nodejs.org/2013/10/18/node-v0-10-21-stable/)
* [Bower 1.2.7](https://npmjs.org/package/bower)
* [Grunt-cli 0.1.11](https://npmjs.org/package/grunt-cli)

## Getting Started

1. Clone the repository from github using either of the links from the [main page](../../..)
2. Copy [configuration.properties.sample](./lumify.properties) file into ```/opt/lumify/config/``` and rename it to configuration.properties.
   * Fill in all empty fields
   * To generate a Google Map V3 api key, please refer to the [documentation](https://developers.google.com/maps/documentation/javascript/tutorial#api_key) provided by Google.
3. Copy [log4j.xml.sample](./log4j.xml) file into ```/opt/lumify/config/``` and rename it to log4j.xml.
4. From the top level project directory, run the command ```mvn clean compile```
5. Deploy storm topology jar files
6. Deploy web war

If you would like to see an example of the ingest capability Lumify has, please refer to our [open source Twitter ingest example](https://github.com/altamiracorp/lumify-twitter) and follow the instructions there.

