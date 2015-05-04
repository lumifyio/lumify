# Lumify Demo Docker Container
The Lumify Demo Docker container encapsulates a minimal deployment of Lumify.  The container is self contained and can be run independant of the development environment, after it is built.  

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

## Installation
Execute the following commands to build the Lumify Demo Docker Container.

```sh
$ boot2docker init -m 8192
$ boot2docker start 
$ cd ~
$ git clone https://github.com/lumifyio/lumify.git
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