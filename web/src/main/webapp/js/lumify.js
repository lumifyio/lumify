
// Debug retina/non-retina by changing to 1/2
// window.devicePixelRatio = 1;

window.requestAnimationFrame = 
    typeof window === 'undefined' ? 
    function(){} : 
    ( window.requestAnimationFrame || 
      window.mozRequestAnimationFrame ||
      window.webkitRequestAnimationFrame ||
      window.msRequestAnimationFrame || function(callback) { setTimeout(callback, 1000/60); } );

require([
    'flight/lib/compose',
    'flight/lib/registry',
    'flight/lib/advice',
    'flight/lib/logger',
    'flight/lib/debug',
    
     // Make underscore available everywhere
    'underscore',

    'util/visibility',

    // Make jQuery plugins available
    'withinScrollable',
    'flightJquery',
    'easing',
    'scrollStop',
    'bootstrap-datepicker',
    'removePrefixedClasses'
],
function(compose, registry, advice, withLogging, debug, _, Visibility) {
    'use strict';

    $.fn.datepicker.defaults.format = "yyyy-mm-dd";
    $.fn.datepicker.defaults.autoclose = true;

    debug.enable(true);
    DEBUG.events.logNone();


    // Uncomment to enable logging of on, off, trigger events
    //DEBUG.events.logAll();

    // Uncomment to enable logging of trigger events
    //DEBUG.events.logByAction('trigger');

    Visibility.attachTo(document);

    var ids = graphVertexIdsToOpen();

    if (ids && ids.length) {
        window.isFullscreenDetails = true;
        $('html').addClass('fullscreenDetails');
        require(['appFullscreenDetails'], function(FullscreenDetailApp) {
            FullscreenDetailApp.attachTo('#app', {
                graphVertexIds: ids
            });
        });
    } else {
        $('html').addClass('fullscreenApp');
        require(['app'], function(App) {
            App.attachTo('#app');
        });
    }


    function graphVertexIdsToOpen() {
        // http://...#v=1,2,3

        var h = location.hash;

        if (!h || h.length === 0) return;

        var m = h.match(/^#?v=(.+)$/);

        if (m && m.length === 2 && m[1].length) {
            return m[1].split(',');
        }
    }
});


