# Pre-built VM Instructions

The Pre-built VM comes pre-installed with a sample dataset. To re-import the data please see [Re-importing data](#re-importing-data).

## System Requirements

The following system requirements must be available/installed for pre-built virtual machine execution:

* At least 4GB of memory (>4GB recommended)
* Virtual Machine software that imports .ova files

## Getting Started

1. Download the [Pre-built VM](http://bits.lumify.io/vm/lumify-opensource-2014-02-06.ova)
2. Import the .ova file into your preferred vm application
3. Start the VM
4. Browse to ```https://localhost:8443```
5. Login to the webapp with username "lumify" and any password to load a prepopulated workspace

If you would like to see an example of the capabilities Lumify has for ingest, please refer to our [open source ingest example](https://github.com/nearinfinity/lumify-twitter) and follow the instructions there.

## DISCLAIMER
* The pre-built VM comes with Maven installed, so you will be able to develop within the VM if you choose to do so.
* To log into your VM, please use the following credentials:

```
username: lumify
password: lumify
```

## Useful URLS

The VM must be running for these URLs to work.

* [Hadoop Administration](http://localhost:50070/dfshealth.jsp)
* [Storm UI](http://localhost:8081/)

