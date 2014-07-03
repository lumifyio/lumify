
define([
    'flight/lib/component',
    'tpl!./scrubber',
    'tpl!./audio'
], function(defineComponent, template, audioTemplate) {
    'use strict';

    return defineComponent(AudioScrubber);

    function AudioScrubber() {

        this.defaultAttrs({
            audioSelector: 'audio'
        });

        this.after('initialize', function() {
            this.$node.html(audioTemplate(
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

            var player = this.select('audioSelector');

            player.on('timeupdate', this.onTimeUpdate.bind(this, player[0]));

            this.on('seekToTime', this.onSeekToTime);
            this.$player = player;
        });

        this.onSeekToTime = function(event, data) {
            var self = this,
                time = data.seekTo / 1000,
                player = this.$player[0];

            if (player.readyState === 4) {
                player.currentTime = time;
                player.play();
            } else {
                this.$player.one('canplay', function() {
                    player.currentTime = time;
                    player.play();
                })
                player.load();
            }
        };

        this.onTimeUpdate = function(player, event) {
            this.trigger('playerTimeUpdate', {
                currentTime: player.currentTime,
                duration: player.duration
            });
        };
    }

    return AudioScrubber;
});
