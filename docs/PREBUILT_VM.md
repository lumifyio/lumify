# Pre-built VM Instructions

The Pre-built VM comes pre-installed with a sample dataset. To re-import the data please see [Re-importing data](#re-importing-data).

## System Requirements

The following system requirements must be available/installed for pre-built virtual machine execution:

* At least 4GB of memory (>4GB recommended)
* Virtual Machine software that imports .ova files

## Getting Started

1. Set up a Host-Only network in your Virtual Machine software called vboxnet0 and configure the ip address to be 192.168.33.1
2. Download a Pre-built VM:
  - [32-bit .ova](http://bits.lumify.io/vm/lumify-demo-opensource-20140321-32bit.ova)
  - [64-bit .ova](http://bits.lumify.io/vm/lumify-demo-opensource-20140321-64bit.ova)
3. Import the .ova file into your preferred vm application
4. Start the VM
5. Browse to ```https://localhost:8443```
6. Login to the webapp with username "lumify" and any password to load a prepopulated workspace

If you would like to see an example of the capabilities Lumify has for ingest, please refer to our [open source ingest example](https://github.com/altamiracorp/lumify-twitter) and follow the instructions there.

## DISCLAIMER
* The pre-built VM comes with Maven installed, so you will be able to develop within the VM if you choose to do so.
* To log into your VM, please use the following credentials:

```
username: lumify
password: lumify
```
    
    ssh lumify@192.168.33.10 


## Useful URLS

The VM must be running for these URLs to work.

* [Hadoop Administration](http://192.168.33.10:50070/dfshealth.jsp)
* [Storm UI](http://192.168.33.10:8081/)

