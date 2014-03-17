

define([
    'flight/lib/component',
    'videojs',
    'tpl!./scrubber',
    'tpl!./video'
], function(defineComponent, videojs, template, videoTemplate) {
    'use strict';

    // TODO: get this from the server
    var NUMBER_FRAMES = 20,
        POSTER = 1,
        FRAMES = 2;

    videojs.options.flash.swf = "/libs/video.js/video-js.swf";

    return defineComponent(VideoScrubber);

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
            if (index == this.currentFrame) {
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

            var self = this,
                userClickedPlayButton = $(event.target).is('.scrubbing-play-button'),
                players = videojs.players,
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

            var scrubPercent = this.scrubPercent;
            _.defer(videojs, video[0], { autoplay:true }, function() {
                var player = this;
                if (!userClickedPlayButton) {
                    player.on("durationchange", durationchange);
                    player.on("loadedmetadata", durationchange);
                }
                player.on("timeupdate", timeupdate);

                function timeupdate(event) {
                    self.trigger('videoTimeUpdate', {
                        currentTime: player.currentTime(),
                        duration: player.duration()
                    });
                }

                function durationchange(event) {
                    var duration = player.duration();
                    if (duration > 0.0 && scrubPercent > 0.0) {
                        player.off('durationchange', durationchange);
                        player.off("loadedmetadata", durationchange);
                        player.currentTime(Math.max(0.0, duration * scrubPercent - 1.0));
                    }
                }
            });
        };

        this.after('initialize', function() {
            var self = this;

            this.$node.toggleClass('allowPlayback', this.attr.allowPlayback)
                      .html(template({}));

            this.select('backgroundScrubberSelector').css({
                width: '100%',
                height: '100%',
                position:'absolute',
                backgroundRepeat: 'no-repeat',
                backgroundImage: 'url(' + this.attr.videoPreviewImageUrl + ')'
            });

            this.select('backgroundPosterSelector').css({
                width: '100%',
                height: '100%',
                position:'absolute',
                backgroundSize: '100%',
                backgroundRepeat: 'no-repeat',
                backgroundPosition: 'left center',
                backgroundImage: 'url(' + this.attr.posterFrameUrl + ')'
            });

            this.showPoster();

            this.on('videoPlayerInitialized', function (e) {
                self.off('mousemove');
                self.off('mouseleave');
                self.off('click');
            });

            this.on('mousemove', {
                scrubbingLineSelector: function(e) { e.stopPropagation(); }
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
        });
    }

    return VideoScrubber;
});
