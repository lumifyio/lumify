#!/bin/bash

protoc -IPATH=src/main/resources/io/lumify/palantir/model --java_out=src/main/java/ src/main/resources/io/lumify/palantir/model/AWState.proto

