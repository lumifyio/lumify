# Lumify Prebuilt VM Instructions

The prebuilt VM includes pre-ingested sample data.


## System Requirements

The following capabilities and system resources are required for prebuilt virtual machine execution:

* virtual machine software that can import an `.ova` file
* at least 2 CPU cores
* at least 4GB of RAM
* at least 8GB of disk space


## Getting Started

1. set up a Host-Only network in your virtual machine software called vboxnet0 and configure the ip address to be 192.168.33.1
2. download the latest `.ova` file for your platform architecture:
  - [32-bit .ova](http://bits.lumify.io/vm/lumify-demo-opensource-20140321-32bit.ova)
  - [64-bit .ova](http://bits.lumify.io/vm/lumify-demo-20140801-64bit.ova)
3. import the .ova file into your preferred virtual machine software
4. start the VM
5. browse to ```https://localhost:8443```
6. login to Lumify with username "lumify" and any password to load a prepopulated workspace


## Developing Lumify

* the prebuilt VM comes with Maven installed so you can develop within the VM if you choose to do so
* log into the VM with the following credentials:

        # username: lumify
        # password: lumify

        ssh lumify@192.168.33.10


## Useful URLS

See [admin-urls.md](admin-urls.md) to monitor the services on which Lumify runs.
