#!/bin/bash

unamestr=`uname`

if [[ "$unamestr" == 'Linux' ]]; then
   sudo docker build -t lumifyio/dev dev
elif [[ "$unamestr" == 'Darwin' ]]; then
   docker build -t lumifyio/dev dev
fi

