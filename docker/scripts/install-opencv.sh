#!/bin/bash -eu

ant_version=1.9.4
ant_tgz=apache-ant-${ant_version}-bin.tar.gz

opencv_version=2.4.9
opencv_zip=opencv-${opencv_version}.zip

cd /tmp
curl -L -O https://bits.lumify.io/extra/${ant_tgz}
tar -xvf ${ant_tgz}

cd /tmp

curl -L -O https://bits.lumify.io/extra/${opencv_zip}
unzip ${opencv_zip}

cd /tmp/opencv-${opencv_version}
sed -i 's/JNI_FOUND/1/g' modules/java/CMakeLists.txt

mkdir /tmp/opencv-${opencv_version}/build
cd /tmp/opencv-${opencv_version}/build
ANT_DIR=/tmp/apache-ant-{$ant_version} cmake -DBUILD_PERF_TESTS=OFF -DBUILD_TESTS=OFF ..
make
sudo make install

sudo ln -s /usr/local/share/OpenCV/java/libopencv_java249.so /usr/local/lib/libopencv_java249.so
sudo ldconfig

rm -rf /tmp/apache-ant-${ant_version}
rm -f /tmp/${ant_tgz}

rm -rf /tmp/opencv-${opencv_version}
rm -f /tmp/${opencv_zip}
