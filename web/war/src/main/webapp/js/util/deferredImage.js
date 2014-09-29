define([], function() {
    'use strict';

    var imageCache = {};

    return deferredImage;

    function deferredImage(url) {
        var cached = imageCache[url];

        if (!cached) {
            cached = imageCache[url] = downloadImage(url);
        }

        return cached;
    }

    function downloadImage(url) {
        var i = new Image(),
            async = $.Deferred();

        i.onload = function() {
            async.resolve(i);
        }
        i.onerror = function() {
            async.reject();
        }

        i.src = url;

        return async;
    }
});
