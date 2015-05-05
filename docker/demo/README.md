# Lumify Demo Docker Container
The Lumify Demo Docker container encapsulates a minimal deployment of Lumify.  The container is self contained and can be run independent of the development environment, after it is built.

## Prerequisites to build from source
The following dependencies must be installed before building the Lumify Demo Docker Container.

- OSX or Linux required to build
- [Java 7 JDK](http://www.oracle.com/technetwork/java/javase/downloads)
- [Maven](https://maven.apache.org/)
- [Git client](http://git-scm.com/)
- OSX build environments
	- [VirtualBox](https://www.virtualbox.org/)
	- [Boot2Docker (includes Docker)](http://boot2docker.io/)
- Linux build environments
	- [Docker](https://www.docker.com/)
- HTML5 compliant web browser

## Install on OSX
Execute the following commands to build the Lumify Demo Docker Container on OSX.

####1. Initialize the Boot2Docker Virtual Machine
Docker requires a Linux kernal and will not run natively on OSX. From an OSX environment you must initialize and start a Boot2Docker virtual machine to host Docker.  

This command initializes a Boot2Docker virtual machine with 8 GB of memory.  You may adjust this parameter to allocate more memory if desired.

```sh
$ boot2docker init -m 8192
```

*\* The Boot2Docker initialization step only needs to be run once.*

####2. Start the Boot2Docker Virtual Machine
This command will start the Boot2Docker virtual machine within VirtualBox.

```sh
$ boot2docker start
```

*\* The Boot2Docker virtual machine only needs to be started if it has not been started or has been stoped.*

####3. Download the Lumify source code
Downlowad the Lumify source code from GitHub to a local directory.

```sh 
$ cd ~
$ git clone https://github.com/lumifyio/lumify.git
```

####4. Build the Lumify Docker Container
Executing the build script will complile the Lumify software and build the Docker container hosting a minimal deployment of Lumify.

```sh
$ cd ~/lumify/docker
$ ./build-demo.sh
```

## Install on Linux
Execute the following commands to build the Lumify Demo Docker Container on Linux.

Docker runs natively on Linux and will have access to the full system resources of the host OS. It is recommended to run the Lumify demo on a machine with 8 GB, or higher, memery.

####1. Download the Lumify source code
Downlowad the Lumify source code from GitHub to a local directory.

```sh 
$ cd ~
$ git clone https://github.com/lumifyio/lumify.git
```

####2. Build the Lumify Docker Container
Executing the build script will complile the Lumify software and build the Docker container hosting a minimal deployment of Lumify.  This step may take a significant amount of time to complete.

```sh
$ cd ~/lumify/docker
$ ./build-demo.sh
```

## Running the demo
Execute the following commnd to run the Lumify Demo Docker Container.

```sh
$ ~/lumify/docker/run-demo.sh   # may need to enter sudo password
```

After Lumify has fully started, open the Lumify web application in an HTML5 compliant browser using the following URL.  You may use any user ID to login to the demo.

```
http://lumify-demo:8080
```

## ssh into lumify-demo
After running the Lumify Docker Container you can 'ssh' into the instance using the following command.

```sh
$ ssh -p 2022 -i <working dir>/lumify/docker/demo/keys/id_rsa root@lumify-demo
```

## Redeploy Lumify to a running Docker container
Execute the following commands to rebuild and deploy the Lumify web application to the currently running Docker container.

```sh
$ ~/lumify/docker/deploy-lumify-demo.sh
```