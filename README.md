# Lumify

![Lumify Logo](web/war/src/main/webapp/img/lumify-logo.png?raw=true)

Lumify is an open source big data analysis and visualization platform. Please see [http://lumify.io](http://lumify.io) for more details and videos.

## Getting Started

To get started quickly, you can try out a hosted installation of Lumify, or build a virtual machine image with Lumify installed and pre-configured.

- [Try Lumify Now](http://lumify.io/try.html)
- [Watch Lumify Videos](https://www.youtube.com/playlist?list=PLDX7b-6_sNA7SCJw5rB9EF0TDpQyrO2XR)

## Lumify Demo Virtual Machine
The following instructions will build a virtual machine that fully encapsulates a minimal Lumify Demo instance.  The virtual machine is configured with 4 CPUs, 8 GB RAM.  The virtual machine does not include any data but does include a minimal ontology.  You may want to try one of the included sample [datasets](datasets) or create your own.

###1. Prerequisites
The following prerequisites must be installed prior to building the Lumify Demo Virtual Machine.
	
- [VirtualBox](https://www.virtualbox.org/)
- [Vagrant](https://www.vagrantup.com/)
- [Git client](http://git-scm.com/)
- [Chrome](http://www.google.com/chrome/) or [Firefox](https://www.mozilla.org/en-US/firefox/new/) browser

###2. Clone the Lumify repo

Checkout the Lumify Source Code using the following Git command.

	$ cd <working dir>
	$ git clone https://github.com/lumifyio/lumify.git

###3. Build and run the Lumfy Demo

The following commands will start the Lumify Demo Virtual Machine.  If the virtual machine has not been built it will be built and then started.

	$ cd <working dir>/lumify
	$ vagrant up demo

Use the following command to add the lumify-demo IP address to your hosts file.

	sudo echo "192.168.33.12  lumify-demo" >> /etc/hosts
	
You can open an ssh shell to the machine as follows

	$ vagrant ssh demo
	
Please see the Vagrant help for other commands that may be useful.

###4. Open Lumify in your web browser

Connect to the Lumify Web App that is running on the Virtual Machine using either Chrome or Firefox and the following URL.

	```
	http://lumify-demo:8080
	```

## Development Quick Start

###1. Prerequisites to build from source

The following dependencies must be installed before building Lumify on the development machine.

- OSX or Linux required to build
- [Java 7 JDK](http://www.oracle.com/technetwork/java/javase/downloads)
- [Maven](https://maven.apache.org/)
- [Git client](http://git-scm.com/)
- [node.js](https://nodejs.org/)
- [Bower](http://bower.io/)
- [Grunt](http://gruntjs.com/)
- [VirtualBox](https://www.virtualbox.org/)
- [Vagrant](https://www.vagrantup.com/)
- Chrome or Firefox web browser

###2. Clone the Lumify repo

    git clone https://github.com/lumifyio/lumify.git
    
   **_This will clone the repo to a `lumify` directory in your current working directory.  This absolute path will be referred to as `<working dir>` for the remainder of these steps._**

###3. Build and run the Lumify Dev Virtual Machine

The Lumify Dev Virtual Machine includes only the backend servers (Hadoop, Accumulo, Elasticsearch, RabbitMQ, Zookeeper) used for development.  This VM makes it easy for developers to get started without needing to install the full stack on thier develoment machines.

The following commands will start the Lumify Dev Virtual Machine.  If the virtual machine has not been built it will be built and then started.

	$ cd <working dir>/lumify
	$ vagrant up dev

Use the following command to add the lumify-dev IP address to your hosts file.

	sudo echo "192.168.33.10  lumify-dev" >> /etc/hosts
	
You can open an ssh shell to the machine as follows

	$ vagrant ssh dev
	
Please see the Vagrant help for other commands that may be useful.

###4. Install the Lumify npm dependencies:
    
    cd <working dir>/lumify/web/war/src/main/webapp
    npm install -g inherits bower grunt
    npm install -g grunt-cli
    
###5. Run the web server from the IDE
   * [Run locally in an IDEA Inellij IDE](docs/ide.md)

See [docs/developer.md](docs/developer.md) for more information on developing for Lumify.

See [datasets](datasets) for sample datasets to get started.

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
