
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

    configureApplication();

    loadApplicationTypeBasedOnUrlHash();

    function configureApplication() {
        // Flight Logging
        debug.enable(true);
        DEBUG.events.logNone();

        // Default datepicker options
        $.fn.datepicker.defaults.format = "yyyy-mm-dd";
        $.fn.datepicker.defaults.autoclose = true;

        Visibility.attachTo(document);
        $(window).on('hashchange', loadApplicationTypeBasedOnUrlHash);
    }

    /**
     * Switch between lumify and lumify-fullscreen-details based on url hash
     */
    function loadApplicationTypeBasedOnUrlHash() {
        var ids = graphVertexIdsToOpen(),

            // Is this the popoout details app? ids passed to hash?
            popoutDetails = !!(ids && ids.length),

            // Is this the default lumify application?
            mainApp = !popoutDetails;

        $('html')
            .toggleClass('fullscreenApp', mainApp)
            .toggleClass('fullscreenDetails', popoutDetails)
        window.isFullscreenDetails = popoutDetails;
        
        if (popoutDetails) {
            require(['appFullscreenDetails'], function(PopoutDetailsApp) {
                PopoutDetailsApp.teardownAll();
                PopoutDetailsApp.attachTo('#app', {
                    graphVertexIds: ids
                });
            });
        } else {
            require(['app'], function(App) {
                App.teardownAll();
                App.attachTo('#app');
            });
        }
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


