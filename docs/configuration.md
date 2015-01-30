
# Configuration

## Configuration search order

By default Lumify will use `io.lumify.core.config.FileConfigurationLoader` to load configuration files.
FileConfigurationLoader will look in the following directories:

* `/opt/lumify/` for Linux/OSX
* `c:/opt/lumify/` for Windows
* `${appdata}/Lumify`
* `${user.home}/.lumify`
* Directory specified by the environment variable `LUMIFY_DIR`

Each of these directories will be searched in order and all files with a `.properties` extension will be
read in alphabetic order. This allows you to override properties in various places.

## Docker

If you are running Lumify processes in Docker the same configuration loading will occur but within the docker
container. This directory is exposed in the `docker/lumify-dev-persistent/opt/lumify` under your lumify source
tree.
