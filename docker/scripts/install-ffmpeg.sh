#!/bin/bash -eu

HOME=/tmp/ffmpeg
SRC=$HOME/src
mkdir $HOME
mkdir $SRC

export PATH=$PATH:$HOME/bin

ARCHIVE_DIR=/tmp/lumify/archives

# setup the archive dir
if [ ! -d "$ARCHIVE_DIR" ]; then
    mkdir -p $ARCHIVE_DIR
fi

# download the archives
if [ ! -f "$ARCHIVE_DIR/lame-3.99.5.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/lame-3.99.5.tar.gz https://bits.lumify.io/extra/lame-3.99.5.tar.gz
fi
if [ ! -f "$ARCHIVE_DIR/libogg-1.3.2.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/libogg-1.3.2.tar.gz https://bits.lumify.io/extra/libogg-1.3.2.tar.gz
fi
if [ ! -f "$ARCHIVE_DIR/libvorbis-1.3.4.tar.gz" ]; then
    curl -L -o $ARCHIVE_DIR/libvorbis-1.3.4.tar.gz https://bits.lumify.io/extra/libvorbis-1.3.4.tar.gz
fi

# extract from the archives
tar xzvf $ARCHIVE_DIR/lame-3.99.5.tar.gz -C $SRC
tar xzvf $ARCHIVE_DIR/libogg-1.3.2.tar.gz -C $SRC
tar xzvf $ARCHIVE_DIR/libvorbis-1.3.4.tar.gz -C $SRC

# delete the archives
rm -rf $ARCHIVE_DIR

# build the packages

# Yasm
cd $SRC
git clone --depth 1 git://github.com/yasm/yasm.git
cd yasm
autoreconf -fiv
./configure --prefix="$HOME/ffmpeg_build" --bindir="$HOME/bin"
make
make install
make distclean


# libx264
cd $SRC
git clone --depth 1 git://git.videolan.org/x264
cd x264
./configure --prefix="$HOME/ffmpeg_build" --bindir="$HOME/bin" --enable-static
make
make install
make distclean


# libfdk_aac
cd $SRC
git clone --depth 1 git://git.code.sf.net/p/opencore-amr/fdk-aac
cd fdk-aac
autoreconf -fiv
./configure --prefix="$HOME/ffmpeg_build" --disable-shared
make
make install
make distclean


# libmp3lame
cd $SRC/lame-3.99.5
./configure --prefix="$HOME/ffmpeg_build" --bindir="$HOME/bin" --disable-shared --enable-nasm
make
make install
make distclean


# libopus
cd $SRC
git clone git://git.opus-codec.org/opus.git
cd opus
autoreconf -fiv
./configure --prefix="$HOME/ffmpeg_build" --disable-shared
make
make install
make distclean


# libogg
cd $SRC/libogg-1.3.2
./configure --prefix="$HOME/ffmpeg_build" --disable-shared
make
make install
make distclean

# libvorbis
cd $SRC/libvorbis-1.3.4
LDFLAGS="-L$HOME/ffmeg_build/lib" CPPFLAGS="-I$HOME/ffmpeg_build/include" ./configure --prefix="$HOME/ffmpeg_build" --with-ogg="$HOME/ffmpeg_build" --disable-shared
make
make install
make distclean


# libvpx
cd $SRC
git clone --depth 1 https://chromium.googlesource.com/webm/libvpx.git
cd libvpx
./configure --prefix="$HOME/ffmpeg_build" --disable-examples
make
make install
make clean


# FFmpeg
cd $SRC
git clone --depth 1 git://source.ffmpeg.org/ffmpeg
cd ffmpeg
PKG_CONFIG_PATH="$HOME/ffmpeg_build/lib/pkgconfig" ./configure --extra-cflags="-I$HOME/ffmpeg_build/include" --extra-ldflags="-L$HOME/ffmpeg_build/lib" --enable-gpl --enable-nonfree --enable-libfdk_aac --enable-libmp3lame --enable-libopus --enable-libvorbis --enable-libvpx --enable-libx264
make
make install
make distclean
hash -r


# qt-faststart
cd $SRC/ffmpeg/tools
make qt-faststart
cp qt-faststart /usr/local/bin
