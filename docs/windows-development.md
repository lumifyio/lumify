# Developing Lumify on Windows

1. Install Docker per their instructions: [https://docs.docker.com/installation](https://docs.docker.com/installation/#installation)

1. Install node and npm per their instructions: [http://nodejs.org/](http://nodejs.org/)

1. Install Python v2.x per their instructions: [https://www.python.org/downloads/](https://www.python.org/downloads/)

1. Install JDK 7 or 8 per their instructions if not already installed: [http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

1. Install Maven 3.2.X per their instructions if not already installed: [http://maven.apache.org/guides/getting-started/windows-prerequisites.html] (http://maven.apache.org/guides/getting-started/windows-prerequisites.html)

1. Install Hadoop 2.2.0
     a. Install the binaries (choose one):
          - Compile install from source: [http://www.srccodes.com/p/article/38/build-install-configure-run-apache-hadoop-2.2.0-microsoft-windows-os](http://www.srccodes.com/p/article/38/build-install-configure-run-apache-hadoop-2.2.0-microsoft-windows-os)
          - Install pre-compiled binaries: [http://bits.lumify.io/extra/hadoop-2.2.0-windows.zip](http://bits.lumify.io/extra/hadoop-2.2.0-windows.zip)
     b. Set the `HADOOP_PATH` environment variable `c:\hadoop-2.2.0`
     c. Append the `PATH` environment variable with `c:\hadoop-2.2.0\bin`

1. Install msysgit via the netinstall option: [https://github.com/msysgit/msysgit/releases](https://github.com/msysgit/msysgit/releases).
**All commands from this point forward are assumed to be running from the msysgit shell, if you need to open the window, run the command c:\mysysgit\msys.bat**

1. Confirm and Add boot2docker, python, java, and maven to $PATH in msysgit bash shell

        // Create or add to file "~/.profile"
        export PATH=$PATH:[python_path]:[boot2docker_path]:[java_path]:[maven_path]

1. Source the file to get the $PATH updates

        source ~/.profile

1. Find the boot2docker VM IP address:

        boot2docker ip

1. Edit `%windir%\system32\drivers\etc\hosts` as an administrator, adding a new line with the IP address from the previous command + space + `lumify-dev`

1. Ensure the Lumify git repo is cloned under your home directory:

        cd /c/Users/{your user name}
        git clone git@github.com:lumifyio/lumify.git

1. Always use Unix-style line-endings:

        git config --global core.autocrlf false

1. SSH into the boot2docker VM:

        boot2docker ssh

1. (Inside of the boot2docker VM) Change directory to your lumify repository:

        cd /c/Users/{your user name}/lumify

1. (Inside of the boot2docker VM) Build the docker image:

        docker/build-dev.sh

1. (Inside of the boot2docker VM) Run the docker image (this will start ZooKeeper, HDFS, YARN, ElasticSearch, and RabbitMQ):

        docker/run-dev.sh

1. Make an npm directory in your roaming profile:

        mkdir -p /c/Users/{your user name}/AppData/Roaming/npm

1. Change to the webapp directory:

        cd /c/Users/{your user name}/lumify/web/war/src/main/webapp

1. Install npm dependencies:

        npm install -g inherits bower grunt-cli
        npm install

1. Install grunt and bower dependencies:

        grunt deps

1. Start watching and recompiling:

        grunt

1. (Optional) create war file:

        cd /c/Users/{your user name}/lumify
        mvn package
