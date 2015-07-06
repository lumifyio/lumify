#!/bin/bash -eu

# wget --header "Cookie: oraclelicense=accept-securebackup-cookie" -O /opt/jdk-7u71-linux-x64.tar.gz http://download.oracle.com/otn-pub/java/jdk/7u71-b14/jdk-7u71-linux-x64.tar.gz
ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archive
if [ ! -f "$ARCHIVE_DIR/jdk-7u71-linux-x64.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/jdk-7u71-linux-x64.tar.gz https://bits.lumify.io/extra/jdk-7u71-linux-x64.tar.gz
fi

# extract from the archive
tar -xzf $ARCHIVE_DIR/jdk-7u71-linux-x64.tar.gz -C /opt

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
ln -s /opt/jdk1.7.0_71 /opt/jdk

JAI_FILE=jai-1_1_3-lib-linux-amd64-jdk.bin
JAI_IMAGEIO_FILE=jai_imageio-1_1-lib-linux-amd64-jdk.bin

(cd /opt/jdk
  curl -L https://bits.lumify.io/extra/java/${JAI_FILE} -O
  sed -i -e 's|more <<EOF|cat > /dev/null <<EOF|' \
         -e 's/agreed=$/agreed=1/' \
         ${JAI_FILE}
  chmod u+x ${JAI_FILE}
  ./${JAI_FILE} && rm ./${JAI_FILE}

  curl -L https://bits.lumify.io/extra/java/${JAI_IMAGEIO_FILE} -O
  chmod u+x ${JAI_IMAGEIO_FILE}
  sed -i -e 's|more <<EOF|cat > /dev/null <<EOF|' \
         -e 's/agreed=$/agreed=1/' \
         -e 's/^tail +/tail -n +/' \
         ${JAI_IMAGEIO_FILE}
  ./${JAI_IMAGEIO_FILE} && rm ./${JAI_IMAGEIO_FILE}
)
