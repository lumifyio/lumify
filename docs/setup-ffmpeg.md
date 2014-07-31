
FFmpeg Setup
============

CentOS
------
1. Follow these instructions to build and install FFmpeg with the necessary plugins
   https://trac.ffmpeg.org/wiki/CompilationGuide/Centos
2. Build and install qt-faststart. (qt-faststart ships with FFMPEG. No seprate
   download is required for it. At this point you should still be in the ffmpeg directory.)

        cd tools
        make qt-faststart
        cp qt-faststart /usr/local/bin

Ubuntu
------
1. Follow these instructions to build and install FFmpeg with the necessary plugins
   https://trac.ffmpeg.org/wiki/CompilationGuide/Ubuntu
2. Build and install qt-faststart. (qt-faststart ships with FFMPEG. No seprate
   download is required for it. At this point you should still be in the ffmpeg directory.)

        cd tools
        make qt-faststart
        cp qt-faststart /usr/local/bin
        
OSX
---
1. Install ffmpeg.
        
        brew install ffmpeg --with-libvorbis --with-libvpx --with-fdk-aac --with-opus --with-theora
        
2. Install qt-faststart

        brew install qtfaststart
