# Pre-built VM Instructions

The Pre-built VM comes pre-installed with a sample dataset. To re-import the data please see [Re-importing data](#re-importing-data).

## System Requirements

The following system requirements must be installed for the pre-built virtual machine execution:

* Any type of Virtual Machine Ware that imports .ova files

## Getting Started

1. Download the [Pre-built VM](http://bits.lumify.io/vm/lumify-2013-12-02.ova)
2. Import the .ova file into your preferred vm application. 
3. Browse to ```http:\\localhost:8080```. 

If you would like to see an example of the capabilities Lumify has for ingest, please refer to our [open source ingest example](https://github.com/nearinfinity/lumify-twitter) and follow the instructions there.

## DISCLAIMER
* The pre-built VM comes with Maven installed, so you will be able to develop within the VM if you choose to do so.
* To log into your VM, please use the following credentials:
```
username: lumify
password: lumify
```

## Useful URLS
* [Hadoop Administration](http://192.168.33.10:50070/dfshealth.jsp)
* [Storm UI](http://192.168.33.10:8080/)

## Re-importing data
1. ```/opt/accumulo-import.sh /opt/lumify/sample-data.tgz```
