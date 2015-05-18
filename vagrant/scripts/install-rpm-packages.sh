#!/bin/bash -eu

rpm -Uhv http://dl.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
yum update -y

#system tools
yum install -y wget curl tar sudo openssh-server openssh-clients git nodejs npm libuuid-devel libtool zip unzip rsync which erlang cmake bison

#ffmpeg
yum install -y lumify-videolan-x264 lumify-fdk-aac lumify-lame lumify-opus lumify-ogg lumify-vorbis lumify-vpx lumify-theora lumify-ffmpeg

#tesseract
yum install -y lumify-leptonica lumify-tesseract lumify-tesseract-eng

#CCExtractor
yum install -y lumify-ccextractor

#OpenCV
yum install -y lumify-opencv

#CMU Sphinx
yum install -y lumify-sphinxbase lumify-pocketsphinx