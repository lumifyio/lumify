#!/bin/bash -eu

ant_version=1.9.4
ant_tgz=apache-ant-${ant_version}-bin.tar.gz

opencv_version=2.4.9
opencv_zip=opencv-${opencv_version}.zip

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archives
if [ ! -f "$ARCHIVE_DIR/${ant_tgz}" ]; then
    curl -L -o $ARCHIVE_DIR/${ant_tgz} https://bits.lumify.io/extra/${ant_tgz}
fi
if [ ! -f "$ARCHIVE_DIR/${opencv_zip}" ]; then
    curl -L -o $ARCHIVE_DIR/${opencv_zip} https://bits.lumify.io/extra/${opencv_zip}
fi

# extract from the archives
tar -xvf $ARCHIVE_DIR/${ant_tgz} -C /tmp
unzip $ARCHIVE_DIR/${opencv_zip} -d /tmp

# delete the archive
rm -rf $ARCHIVE_DIR

# build the package
cd /tmp/opencv-${opencv_version}
sed -i 's/JNI_FOUND/1/g' modules/java/CMakeLists.txt

mkdir /tmp/opencv-${opencv_version}/build
cd /tmp/opencv-${opencv_version}/build
ANT_EXECUTABLE=/tmp/apache-ant-{$ant_version}/bin/ant cmake -DBUILD_PERF_TESTS=OFF -DBUILD_TESTS=OFF ..
make
make install

ln -s /usr/local/share/OpenCV/java/libopencv_java249.so /usr/local/lib/libopencv_java249.so
ldconfig

# delete the src dirs
rm -rf /tmp/apache-ant-${ant_version}
rm -rf /tmp/opencv-${opencv_version}
