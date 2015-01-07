#!/bin/bash

protoc -IPATH=src/main/resources/io/lumify/palantir/dataImport/model --java_out=src/main/java/ src/main/resources/io/lumify/palantir/dataImport/model/AWState.proto

