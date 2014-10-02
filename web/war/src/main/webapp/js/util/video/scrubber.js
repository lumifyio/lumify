
define([
    'flight/lib/component',
    'videojs',
    'tpl!./scrubber',
    'tpl!./video',
    'util/detectedObjects/withFacebox'
], function(defineComponent, videojs, template, videoTemplate, withFacebox) {
    'use strict';

    // TODO: get this from the server
    var NUMBER_FRAMES = 20,
        POSTER = 1,
        FRAMES = 2;

    videojs.options.flash.swf = '/libs/video.js/dist/video-js/video-js.swf';

    return defineComponent(VideoScrubber, withFacebox);

    function VideoScrubber() {

        this.showing = 0;
        this.currentFrame = -1;

        this.defaultAttrs({
            allowPlayback: false,
            backgroundPosterSelector: '.background-poster',
            backgroundScrubberSelector: '.background-scrubber',
            scrubbingLineSelector: '.scrubbing-line',
            videoSelector: 'video'
        });

        this.showFrames = function(index) {
            if (index == this.currentFrame || !this.attr.videoPreviewImageUrl) {
                return;
            }

            var width = this.$node.width();

            this.select('scrubbingLineSelector').css({
                left: (index / NUMBER_FRAMES) * width
            }).show();

            var css = {
                backgroundSize: (width * NUMBER_FRAMES) + 'px auto',
                backgroundPosition: (width * (index||0) * -1) + 'px center'
            };

            this.select('backgroundScrubberSelector').css(css).show();
            this.select('backgroundPosterSelector').hide();
            this.showing = FRAMES;
            this.currentFrame = index;

            this.trigger('scrubberFrameChange', {
               index: index,
               numberOfFrames: NUMBER_FRAMES
            });
        };

        this.showPoster = function() {
            this.select('scrubbingLineSelector').hide();
            this.select('backgroundScrubberSelector').hide();
            this.select('backgroundPosterSelector').show();

            this.showing = POSTER;
            this.currentFrame = -1;

            this.trigger('scrubberFrameChange', {
               index: 0,
               numberOfFrames: NUMBER_FRAMES
            });
        };

        this.onClick = function(event) {
            if (this.attr.allowPlayback !== true || this.select('videoSelector').length) {
                return;
            }

            var userClickedPlayButton = $(event.target).is('.scrubbing-play-button');

            if (userClickedPlayButton) {
                this.startVideo();
            } else {
                this.startVideo({
                    percentSeek: this.scrubPercent
                })
            }
        };

        this.startVideo = function(opts) {
            var self = this,
                options = opts || {},
                $video = this.select('videoSelector'),
                videoPlayer = $video.length && $video[0];

            if (videoPlayer && videoPlayer.readyState === 4) {
                videoPlayer.currentTime = Math.max(0.0,
                       (options.percentSeek ?
                            options.percentSeek * videoPlayer.duration :
                            options.seek ? options.seek : 0.0
                       ) - 1.0
                );
                videoPlayer.play();
            } else {
                var players = videojs.players,
                    video = $(videoTemplate(
                        _.tap(this.attr, function(attrs) {
                            var url = attrs.rawUrl;
                            if (~url.indexOf('?')) {
                                url += '&';
                            } else {
                                url += '?';
                            }
                            url += 'playback=true';
                            attrs.url = url;
                        })
                    ));

                this.$node.html(video);
                Object.keys(players).forEach(function(player) {
                    if (players[player]) {
                        players[player].dispose();
                        delete players[player];
                    }
                });

                this.trigger('videoPlayerInitialized');

                _.defer(videojs, video[0], {
                    controls: true,
                    autoplay: true,
                    preload: 'auto'
                }, function() {
                    var $this = this;

                    if (options.seek || options.percentSeek) {
                        $this.on('durationchange', durationchange);
                        $this.on('loadedmetadata', durationchange);
                    }
                    $this.on('timeupdate', timeupdate);

                    function timeupdate(event) {
                        self.trigger('playerTimeUpdate', {
                            currentTime: $this.currentTime(),
                            duration: $this.duration()
                        });
                    }

                    function durationchange(event) {
                        var duration = $this.duration();
                        if (duration > 0.0) {
                            $this.off('durationchange', durationchange);
                            $this.off('loadedmetadata', durationchange);
                            $this.currentTime(
                                Math.max(0.0,
                                    (options.percentSeek ?
                                        duration * scrubPercent :
                                        options.seek) - 1.0
                                )
                            );
                        }
                    }
                });
            }
        };

        this.after('initialize', function() {
            var self = this;

            this.$node
                .toggleClass('disableScrubbing', !this.attr.videoPreviewImageUrl)
                .toggleClass('allowPlayback', this.attr.allowPlayback)
                      .html(template({}));

            if (this.attr.videoPreviewImageUrl) {
                this.on('DetectedObjectEnter', this.onVideoDetectedObjectEnter);
                this.on('DetectedObjectLeave', this.onVideoDetectedObjectLeave);
                this.initializeFacebox(
                    this.select('backgroundScrubberSelector').css({
                        width: '100%',
                        height: '100%',
                        position: 'absolute',
                        backgroundRepeat: 'no-repeat',
                        backgroundImage: 'url(' + this.attr.videoPreviewImageUrl + ')'
                    })
                );
            }

            this.select('backgroundPosterSelector').css({
                width: '100%',
                height: '100%',
                position: 'absolute',
                backgroundSize: '100%',
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'left center',
                backgroundImage: 'url(' + this.attr.posterFrameUrl + ')'
            });

            this.showPoster();

            this.on('videoPlayerInitialized', function(e) {
                self.off('mousemove');
                self.off('mouseleave');
                self.off('click');
            });

            this.on('mousemove', {
                scrubbingLineSelector: function(e) {
                    e.stopPropagation();
                }
            });
            this.$node
                .on('mouseenter mousemove', function(e) {
                    if ($(e.target).is('.scrubbing-play-button')) {
                        e.stopPropagation();
                        self.showPoster();
                    } else {
                        var left = e.pageX - $(e.target).closest('.preview').offset().left,
                            percent = left / this.offsetWidth,
                            index = Math.round(percent * NUMBER_FRAMES);

                        self.scrubPercent = index / NUMBER_FRAMES;
                        self.showFrames(index);
                    }
                })
                .on('mouseleave', function(e) {
                    self.showPoster();
                })
                .on('click', self.onClick.bind(self));

            this.on('seekToTime', this.onSeekToTime);
        });

        this.onSeekToTime = function(event, data) {
            this.startVideo({
                seek: data.seekTo / 1000
            });
        };

        this.onVideoDetectedObjectEnter = function(event, data) {
        };

        this.onVideoDetectedObjectLeave = function(event, data) {
            this.showPoster();
        };
    }

    return VideoScrubber;
});
