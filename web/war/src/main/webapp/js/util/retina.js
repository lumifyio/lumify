
define([], function() {
    'use strict';

    function Retina() {

        var self = this,
            properties = 'x y z w h'.split(' '),
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
                $(document).trigger('devicePixelRatioChanged', { devicePixelRatio: newRatio });
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
            if (!position) {
                return {
                    x: 0,
                    y: 0
                };
            }

            var obj = {};
            properties.forEach(function(propertyName) {
                if (propertyName in position) {
                    obj[propertyName] = position[propertyName] / self.devicePixelRatio;
                }
            })

            return obj;
        };

        this.pointsToPixels = function(position) {
            if (!position) {
                return {
                    x: 0,
                    y: 0
                };
            }

            var obj = {};
            properties.forEach(function(propertyName) {
                if (propertyName in position) {
                    obj[propertyName] = position[propertyName] * self.devicePixelRatio;
                }
            })

            return obj;
        };

        observeRatioChanges(this.onRatioChange.bind(this));
    }

    return new Retina();
});
