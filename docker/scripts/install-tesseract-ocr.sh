#!/bin/bash

#
# Build Script for making standalone version of Tesseract
# Wes Fowlks
# 10/01/2014
# Originally posted at:https://code.google.com/p/tesseract-ocr/issues/detail?id=1326
#

BUILD_ZLIB=1
BUILD_LIBJPEG=1
BUILD_LIBPNG=1
BUILD_LEPTONICA=1
BUILD_TESSERACT=1

# Get the base directory of where the script is
BASE_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD_DIR=$BASE_DIR/build
ARCHIVE_DIR=$BASE_DIR/archives
SRC_DIR=$BASE_DIR/src
TESSERACT_DIR=$BASE_DIR/tesseract

#Library Versions
ZLIB_VERSION=1.2.8
LIBPNG_VERSION=1.6.13
LIBJPEG_VERSION=9a
LEPTONICA_VERSION=1.71
TESSERACT_VERSION=3.02.02

echo "Base Build Directory: " $BUILD_DIR

# Functions usefull throughtout the script
function setupDirs() {
        if [ ! -d "$ARCHIVE_DIR" ]; then
                mkdir $ARCHIVE_DIR
        fi

        if [ ! -d "$SRC_DIR" ]; then
                mkdir $SRC_DIR
        fi

        if [ ! -d "$BUILD_DIR" ]; then
                mkdir $BUILD_DIR
        fi
}

# First check to see if zlib
if [ $BUILD_ZLIB = 1 ]
then
        echo "Building ZLIB"
        setupDirs

        # Clean up old files
        rm -rf $SRC_DIR/zlib* $BUILD_DIR/zlib*

        if [ ! -f "$ARCHIVE_DIR/zlib-$ZLIB_VERSION.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/zlib-$ZLIB_VERSION.tar.gz https://bits.lumify.io/extra/zlib-$ZLIB_VERSION.tar.gz
        fi

        echo "Extracting archive"
        tar -xzf $ARCHIVE_DIR/zlib-$ZLIB_VERSION.tar.gz -C $SRC_DIR

        cd "$SRC_DIR/zlib-$ZLIB_VERSION"

        echo "Configuring ZLIB for Standalone"
        ./configure --solo --static

        echo "Building Zlib and deploying to $BUILD_DIR"
        make install prefix=$BUILD_DIR

        #Check if the build was successful
        if [ -f "$BUILD_DIR/include/zlib.h" ]; then
                echo "ZLIB Build Successful"
        else
                echo "ZLIB build failed. Exiting."
                exit 1
        fi

else
        echo "Skipping ZLib"
fi

# Build Libjpeg
if [ $BUILD_LIBJPEG = 1 ]
then

        echo "Building Lib Jpeg"
        setupDirs

        # Clean up old files
        rm -rf $SRC_DIR/jpeg* $BUILD_DIR/jpeg*

        if [ ! -f "$ARCHIVE_DIR/jpegsrc.v$LIBJPEG_VERSION.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/jpeg.v$LIBJPEG_VERSION.tar.gz https://bits.lumify.io/extra/jpeg.v$LIBJPEG_VERSION.tar.gz
        fi

        echo "Extracting archive"
        tar -xzf $ARCHIVE_DIR/jpeg.v$LIBJPEG_VERSION.tar.gz -C $SRC_DIR

        cd "$SRC_DIR/jpeg-$LIBJPEG_VERSION"

        echo "Configuring Lib Jpeg for Standalone"
        ./configure --disable-shared --prefix=$BUILD_DIR

        echo "Building LIBJPEG and deploying to $BUILD_DIR"
        make install

        #Check if the build was successful
        if [ -f "$BUILD_DIR/include/jpeglib.h" ]; then
                echo "LIB JPEG Build Successful"
        else
                echo "LIBJPEG build failed. Exiting."
                exit 1
        fi

else
        echo "Skipping LIBJPEG"
fi

# Build Lib PNG
if [ $BUILD_LIBPNG = 1 ]
then
        echo "Building Lib PNG"
        setupDirs

        # Clean up old files
        rm -rf $SRC_DIR/libpng* $BUILD_DIR/libpng*

        if [ ! -f "$ARCHIVE_DIR/libpng-$LIBPNG_VERSION.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/libpng-$LIBPNG_VERSION.tar.gz https://bits.lumify.io/extra/libpng-1.6.13.tar.gz
        fi

        echo "Extracting archive"
        tar -xzf $ARCHIVE_DIR/libpng-$LIBPNG_VERSION.tar.gz -C $SRC_DIR

        cd "$SRC_DIR/libpng-$LIBPNG_VERSION"

        echo "Copying libz header files to libpng"
        cp $BUILD_DIR/include/zlib.h .
        cp $BUILD_DIR/include/zconf.h .

        echo "Configuring Lib PNG for Standalone"
        LDFLAGS="-L$BUILD_DIR/lib -lz" CPPFLAGS="-I$BUILD_DIR/include" ./configure --prefix=$BUILD_DIR --enable-shared=no

        echo "Building LIBPNG and deploying to $BUILD_DIR"
        make check
        make install

        #Check if the build was successful
        if [ -f "$BUILD_DIR/include/libpng16/png.h" ]; then
                echo "LIB PNG Build Successful"
        else
                echo "LIBPNG build failed. Exiting."
                exit 1
        fi

else
        echo "Skipping LIBPNG"
fi

# Build Leptonica
if [ $BUILD_LEPTONICA = 1 ]
then
        echo "Building Leptonica"
        setupDirs

        # Clean up old files
        rm -rf $SRC_DIR/leptonica* $BUILD_DIR/leptonica*

        if [ ! -f "$ARCHIVE_DIR/leptonica-$LEPTONICA_VERSION.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/leptonica-$LEPTONICA_VERSION.tar.gz https://bits.lumify.io/extra/leptonica-$LEPTONICA_VERSION.tar.gz
        fi

        echo "Extracting archive"
        tar -xzf $ARCHIVE_DIR/leptonica-$LEPTONICA_VERSION.tar.gz -C $SRC_DIR

        cd "$SRC_DIR/leptonica-$LEPTONICA_VERSION"

        echo "Configuring leptonica for standalone"
        ./make-for-local

        echo "Modifying environ.h"
        cat src/environ.h |sed -e 's/#define  HAVE_LIBTIFF     1/#define  HAVE_LIBTIFF     0/g' > src/environ.test.h
        mv src/environ.test.h src/environ.h

        echo "Copying dependencies to leptonica"
        cp -r $BUILD_DIR/include src
        cd src

        echo "Building LEPTONICA and deploying to $BUILD_DIR"
        make EXTRAINCLUDES="-I./include -I./include/libpng16"

        #Check if the build was successful
        if [ -f "$SRC_DIR/leptonica-$LEPTONICA_VERSION/lib/nodebug/liblept.a" ]; then
                echo "Leptonica Build Successful"
        else
                echo "LEPTONICA build failed. Exiting."
                exit 1
        fi

        echo "Copying files for Tesseract"
        cp $SRC_DIR/leptonica-$LEPTONICA_VERSION/lib/nodebug/liblept.a $BUILD_DIR/lib

        if [ ! -f "$BUILD_DIR/include/leptonica" ]; then
                mkdir $BUILD_DIR/include/leptonica
        fi

        cp $SRC_DIR/leptonica-$LEPTONICA_VERSION/src/*.h $BUILD_DIR/include/leptonica

else
        echo "Skipping Leptonica"
fi

# Build Tesseract
if [ $BUILD_TESSERACT = 1 ]
then

        echo "Building Tesseract"
        rm -rf $SRC_DIR/tesseract*

        #Create Tesseract Build Directory
        if [ ! -d "$TESSERACT_DIR" ]; then
                mkdir $TESSERACT_DIR
        else
                rm -rf $TESSERACT_DIR/*
        fi

        if [ ! -f "$ARCHIVE_DIR/tesseract-ocr-$TESSERACT_VERSION.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/tesseract-ocr-$TESSERACT_VERSION.tar.gz https://bits.lumify.io/extra/tesseract-ocr-3.02.02.tar.gz
        fi

        echo "Extracting archive"
        tar -xzf $ARCHIVE_DIR/tesseract-ocr-$TESSERACT_VERSION.tar.gz -C $SRC_DIR
        cd "$SRC_DIR/tesseract-ocr"

        echo "Configuring Tesseract"
        CXXFLAGS="-I$BUILD_DIR/include -I$BUILD_DIR/include/libpng16 -I$BUILD_DIR/include/leptonica" LIBLEPT_HEADERSDIR="$BUILD_DIR/include/leptonica" LIBS="-lpng -ljpeg -lz" LDFLAGS="-L$BUILD_DIR/lib" ./configure --prefix=$TESSERACT_DIR --disable-tessdata-prefix --enable-shared=no

        echo "Configuration Configuration done, now Building"
        make install

        ls $TESSERACT_DIR/bin

        if [ -x "$TESSERACT_DIR/bin/tesseract" ]; then
                echo "Tesseract Build Successful"
        else
                echo "Tesseract build failed. Exiting."
                exit 1
        fi

        echo "Checking the language files"
        if [ ! -f "$ARCHIVE_DIR/tesseract-ocr-3.02.eng.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/tesseract-ocr-3.02.eng.tar.gz https://bits.lumify.io/extra/tesseract-ocr-3.02.eng.tar.gz
        fi

        echo "Checking OSD (Optical Script Detection) models"
        if [ ! -f "$ARCHIVE_DIR/tesseract-ocr-3.01.osd.tar.gz" ]; then
                #Download the file
                curl -L -o $ARCHIVE_DIR/tesseract-ocr-3.01.osd.tar.gz https://bits.lumify.io/extra/tesseract-ocr-3.01.osd.tar.gz
        fi

        echo "Installing Languages and OSD"
        tar -xzf $ARCHIVE_DIR/tesseract-ocr-3.02.eng.tar.gz -C $TESSERACT_DIR/bin
        tar -xzf $ARCHIVE_DIR/tesseract-ocr-3.01.osd.tar.gz -C $TESSERACT_DIR/bin

        cd $TESSERACT_DIR/bin

        echo "Tesseract is now built and can be found at: $BUILD_DIR"

else
        echo "Skipping Tesseract"
fi