
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
    'jquery', 
    'jqueryui',
    'bootstrap',
    'es5shim',
    'es5sham',

    'flight/lib/compose',
    'flight/lib/registry',
    'flight/lib/advice',
    'flight/lib/logger',
    'flight/lib/debug',
    
     // Make underscore available everywhere
    'underscore',

    'util/visibility',
    'service/user',

    'easing',
    'scrollStop',
    'bootstrap-datepicker',
    'util/jquery.flight',
    'util/jquery.removePrefixedClasses'
],
function(jQuery, jQueryui, bootstrap, es5shim, es5sham, compose, registry, advice, withLogging, debug, _, Visibility, UserService) {
    'use strict';

    configureApplication();

    function configureApplication() {
        // Flight Logging
        debug.enable(true);
        DEBUG.events.logNone();

        // Default datepicker options
        $.fn.datepicker.defaults.format = "yyyy-mm-dd";
        $.fn.datepicker.defaults.autoclose = true;

        Visibility.attachTo(document);
        $(window).on('hashchange', loadApplicationTypeBasedOnUrlHash);

        loadApplicationTypeBasedOnUrlHash();
    }

    /**
     * Switch between lumify and lumify-fullscreen-details based on url hash
     */
    function loadApplicationTypeBasedOnUrlHash(e) {
        var ids = graphVertexIdsToOpen(),

            // Is this the popoout details app? ids passed to hash?
            popoutDetails = !!(ids && ids.length),

            // If this is a hash change
            event = e && e.originalEvent,

            // Is this the default lumify application?
            mainApp = !popoutDetails;

        // Ignore hash change if not in popout and not going to popout
        if (event && !isPopoutUrl(event.newURL) && !isPopoutUrl(event.oldURL)) {
            return;
        }

        new UserService().isLoginRequired()
            .done(function() {
                attachApplication(false);
            })
            .fail(function() {
                attachApplication(true);
            });


        function attachApplication(loginRequired) {
            $('html')
                .toggleClass('fullscreenApp', mainApp)
                .toggleClass('fullscreenDetails', popoutDetails)
            window.isFullscreenDetails = popoutDetails;
            

            if (loginRequired) {
                require(['login'], function(Login) {
                    Login.teardownAll();
                    Login.attachTo('#login');
                });
            } else if (popoutDetails) {
                $('#login').remove();
                require(['appFullscreenDetails'], function(PopoutDetailsApp) {
                    PopoutDetailsApp.teardownAll();
                    PopoutDetailsApp.attachTo('#app', {
                        graphVertexIds: ids
                    });
                });
            } else {
                $('#login').remove();
                require(['app'], function(App) {
                    if (event) {
                        location.replace(location.href);
                    } else {
                        App.teardownAll();
                        App.attachTo('#app');
                    }
                });
            }
        }
    }

    function isPopoutUrl(url) {
        var ids = graphVertexIdsToOpen(url)

        return !!(ids && ids.length);
    }

    function graphVertexIdsToOpen(url) {
        // http://...#v=1,2,3

        var h = location.hash;

        if (url) {
            var urlMatch = url.match(/.*?(#.*)$/);
            if (urlMatch) {
                h = urlMatch[1];
            } else h = '';
        }

        if (!h || h.length === 0) return;

        var m = h.match(/^#?v=(.+)$/);

        if (m && m.length === 2 && m[1].length) {
            return m[1].split(',');
        }
    }
});


