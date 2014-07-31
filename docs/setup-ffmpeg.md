# FFmpeg Setup

## CentOS

1. Follow the [CentOS instructions](https://trac.ffmpeg.org/wiki/CompilationGuide/Centos) to build and install FFmpeg with the necessary plugins
2. Build and install qt-faststar (qt-faststart ships with FFmpeg -- no separate download is required)

*in the ffmpeg directory:*

        cd tools
        make qt-faststart
        cp qt-faststart /usr/local/bin


## Ubuntu

1. Follow the [Ubuntu instructions](https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu) to build and install FFmpeg with the necessary plugins
2. Build and install qt-faststar (qt-faststart ships with FFmpeg -- no separate download is required)

*in the ffmpeg directory:*

        cd tools
        make qt-faststart
        cp qt-faststart /usr/local/bin


## OSX

1. Install FFmpeg
        
        brew install ffmpeg --with-libvorbis --with-libvpx --with-fdk-aac --with-opus --with-theora
        
2. Install qt-faststart

        brew install qtfaststart
