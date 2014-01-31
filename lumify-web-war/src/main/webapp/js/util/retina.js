
define([], function() {
    'use strict';

    function Retina() {

        var self = this,
            zoomSvg,
            getZoomRatio = function() {
                if (!zoomSvg) {
                    zoomSvg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
                    zoomSvg.setAttribute('xmlns', 'http://www.w3.org/2000/svg');
                    zoomSvg.setAttribute('version', '1.1');
                    zoomSvg.style.position = 'absolute';
                    zoomSvg.style.left = '-9999px';
                    zoomSvg.style.width = '1px';
                    zoomSvg.style.height = '1px';
                    document.body.appendChild(zoomSvg);
                }
                return zoomSvg.currentScale;
            },
            getRatio = function() {
                return ('devicePixelRatio' in window ? devicePixelRatio : 1) / getZoomRatio();
            },
            updateRatio = function(newRatio) {
                self.devicePixelRatio = newRatio;
                $(document).trigger('devicePixelRatioChanged', { devicePixelRatio:newRatio });
            },
            observeRatioChanges = function(callback) {
                if ('matchMedia' in window) {
                    matchMedia('(-webkit-device-pixel-ratio:1)').addListener(callback);
                }
            };


        this.devicePixelRatio = getRatio();
        this.onRatioChange = function() {
            updateRatio(getRatio());
        };
        this.pixelsToPoints = function(position) {
            if(!position) {
                return {
                    x: 0,
                    y: 0
                };
            }
            return {
                x: position.x / self.devicePixelRatio,
                y: position.y / self.devicePixelRatio
            };
        };
        this.pointsToPixels = function(position) {
            if(!position) {
                return {
                    x: 0,
                    y: 0
                };
            }
            return {
                x: position.x * self.devicePixelRatio,
                y: position.y * self.devicePixelRatio
            };
        };

        observeRatioChanges(this.onRatioChange.bind(this));
    }


    return new Retina();
});
