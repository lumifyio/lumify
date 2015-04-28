#!/bin/bash -eu

# wget --header "Cookie: oraclelicense=accept-securebackup-cookie" -O /opt/jdk-7u71-linux-x64.tar.gz http://download.oracle.com/otn-pub/java/jdk/7u71-b14/jdk-7u71-linux-x64.tar.gz
curl -L -o /opt/jdk-7u71-linux-x64.tar.gz https://bits.lumify.io/extra/jdk-7u71-linux-x64.tar.gz
tar -xzf /opt/jdk-7u71-linux-x64.tar.gz -C /opt
rm /opt/jdk-7u71-linux-x64.tar.gz
ln -s /opt/jdk1.7.0_71 /opt/jdk

JAI_VERSION=1.1.3
JAI_IMAGEIO_VERSION=1.1

(cd /opt/jdk
  curl -L http://download.java.net/media/jai/builds/release/$(echo ${JAI_VERSION} | sed -e 's/\./_/g')/jai-$(echo ${JAI_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin -O
  sed -i -e 's|more <<EOF|cat > /dev/null <<EOF|' \
         -e 's/agreed=$/agreed=1/' \
         jai-$(echo ${JAI_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin
  chmod u+x jai-$(echo ${JAI_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin
  ./jai-$(echo ${JAI_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin && rm ./jai-$(echo ${JAI_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin

  curl -L http://download.java.net/media/jai-imageio/builds/release/${JAI_IMAGEIO_VERSION}/jai_imageio-$(echo ${JAI_IMAGEIO_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin -O
  chmod u+x jai_imageio-$(echo ${JAI_IMAGEIO_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin
  sed -i -e 's|more <<EOF|cat > /dev/null <<EOF|' \
         -e 's/agreed=$/agreed=1/' \
         -e 's/^tail +/tail -n +/' \
         jai_imageio-$(echo ${JAI_IMAGEIO_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin
  ./jai_imageio-$(echo ${JAI_IMAGEIO_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin && rm jai_imageio-$(echo ${JAI_IMAGEIO_VERSION} | sed -e 's/\./_/g')-lib-linux-amd64-jdk.bin
)
