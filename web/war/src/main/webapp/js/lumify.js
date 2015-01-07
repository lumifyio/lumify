
// Debug retina/non-retina by changing to 1/2
// window.devicePixelRatio = 1;

// Call this function and reload to enable liveReload when `grunt` is running
window.enableLiveReload = function(enable) {
    if ('localStorage' in window) {
        if (enable === true || typeof enable === 'undefined') {
            console.debug('Enabling LiveReload...')
            require(['//localhost:35729/livereload.js'], function() {
                console.debug('LiveReload successfully enabled');
            });
            localStorage.setItem('liveReloadEnabled', true);
        } else {
            console.debug('Disabling LiveReload')
            localStorage.removeItem('liveReloadEnabled');
        }
    }
}

window.switchLanguage = function(code) {
    var availableLocales = 'en es de fr it zh_TW'.split(' ');

    if (~availableLocales.indexOf(code)) {
        var parts = code.split('_');
        if (parts[0]) {
            localStorage.setItem('language', parts[0]);
        }
        if (parts[1]) {
            localStorage.setItem('country', parts[1]);
        }
        if (parts[2]) {
            localStorage.setItem('variant', parts[2]);
        }
        location.reload();
    } else console.error('Available Locales: ' + availableLocales.join(', '));
}

if ('localStorage' in window) {
    if (localStorage.getItem('liveReloadEnabled')) {
        enableLiveReload(true);
    }
}
window.gremlins = function() {
    require(['gremlins'], function(gremlins) {
        gremlins.createHorde()
        .gremlin(gremlins.species.formFiller())
        .gremlin(gremlins.species.clicker())
        .gremlin(
            gremlins.species.clicker()
            .clickTypes(['click'])
            .canClick(function(element) {
                return $(element).is('button,a,li') &&
                    $(element).closest('.logout').length === 0;
            }))
        .gremlin(gremlins.species.typer())
        .mogwai(gremlins.mogwais.fps())
        .unleash({nb: 1000});
    })
}

window.requestAnimationFrame =
    typeof window === 'undefined' ?
    function() { } :
    (
        window.requestAnimationFrame ||
        window.mozRequestAnimationFrame ||
        window.webkitRequestAnimationFrame ||
        window.msRequestAnimationFrame ||
        function(callback) {
            setTimeout(callback, 1000 / 60);
        }
    );
window.TRANSITION_END = 'transitionend webkitTransitionEnd MSTransitionEnd oTransitionEnd otransitionend';
window.ANIMATION_END = 'animationend webkitAnimationEnd MSAnimationEnd oAnimationEnd oanimationend';

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
    'util/privileges',

    'easing',
    'scrollStop',
    'bootstrap-datepicker',
    'bootstrap-timepicker',
    'util/jquery.flight',
    'util/jquery.removePrefixedClasses',

    'util/promise'
],
function(jQuery,
         jQueryui,
         bootstrap,
         es5shim,
         es5sham,
         compose,
         registry,
         advice,
         withLogging,
         debug,
         _,
         Visibility,
         Privileges) {
    'use strict';

    var App, FullScreenApp, Login, F, withDataRequest;

    require(['data/data'], configureApplication);

    function configureApplication(Data) {
        // Flight Logging
        try {
            debug.enable(false);
            DEBUG.events.logNone();
        } catch(e) {
            console.warn('Error enabling DEBUG mode for flight, probably because Safari Private Browsing enabled', e);
        }

        // Default templating
        _.templateSettings.escape = /\{([\s\S]+?)\}/g;
        _.templateSettings.evaluate = /<%([\s\S]+?)%>/g;
        _.templateSettings.interpolate = /\{-([\s\S]+?)\}/g;

        // Default datepicker options
        $.fn.datepicker.defaults.format = 'yyyy-mm-dd';
        $.fn.datepicker.defaults.autoclose = true;

        Data.attachTo(document);
        Visibility.attachTo(document);
        Privileges.attachTo(document);
        document.dispatchEvent(new Event('readyForPlugins'));
        $(window).on('hashchange', loadApplicationTypeBasedOnUrlHash);

        require([
            'util/messages',
            'util/vertex/urlFormatters',
            'util/withDataRequest',
            'util/handlebars/helpers'
        ], function(i18n, _F, _withDataRequest) {
            window.i18n = i18n;
            F = _F;
            withDataRequest = _withDataRequest;
            loadApplicationTypeBasedOnUrlHash();
        });
    }

    /**
     * Switch between lumify and lumify-fullscreen-details based on url hash
     */
    function loadApplicationTypeBasedOnUrlHash(e) {
        var toOpen = F.vertexUrl.parametersInUrl(location.href),

            ids = toOpen && toOpen.vertexIds,

            workspaceId = toOpen && toOpen.workspaceId,

            // Is this the popoout details app? ids passed to hash?
            popoutDetails = !!(toOpen && toOpen.type === 'FULLSCREEN' && ids.length),

            // If this is a hash change
            event = e && e.originalEvent,

            // Is this the default lumify application?
            mainApp = !popoutDetails;

        if (event && isAddUrl(event.oldURL) && isMainApp(event.newURL)) {
            return;
        }

        if (event && isPopoutUrl(event.oldURL) && isPopoutUrl(event.newURL)) {
            return $('#app').trigger('vertexUrlChanged', {
                graphVertexIds: ids,
                workspaceId: workspaceId
            });
        }

        withDataRequest.dataRequest('user', 'me')
            .then(function(user) {
                attachApplication(false);
            })
            .catch(function() {
                attachApplication(true, '', {});
            })

        function attachApplication(loginRequired, message, options) {
            var user = !loginRequired && lumifyData.currentUser;

            if (!event) {
                $('html')
                    .toggleClass('fullscreenApp', mainApp)
                    .toggleClass('fullscreenDetails', popoutDetails);

                window.isFullscreenDetails = popoutDetails;
            }

            if (loginRequired) {
                require(['login'], function(Login) {
                    Login.teardownAll();
                    Login.attachTo('#login', {
                        errorMessage: message,
                        errorMessageOptions: options,
                        toOpen: toOpen
                    });
                });
            } else if (popoutDetails) {
                $('#login').remove();
                require(['appFullscreenDetails'], function(comp) {
                    if (event) {
                        location.reload();
                    } else {
                        if (App) {
                            App.teardownAll();
                        }
                        FullScreenApp = comp;
                        FullScreenApp.teardownAll();
                        FullScreenApp.attachTo('#app', {
                            graphVertexIds: ids,
                            workspaceId: workspaceId
                        });
                    }
                });
            } else {
                $('#login').remove();
                require(['app'], function(comp) {
                    App = comp;
                    if (event) {
                        location.reload();
                    } else {
                        if (FullScreenApp) {
                            FullScreenApp.teardownAll();
                        }
                        App.teardownAll();
                        var options = {};
                        if (toOpen && toOpen.type === 'ADD' && ids.length) {
                            options.addVertexIds = toOpen;
                        }
                        if (toOpen && toOpen.type === 'ADMIN' && toOpen.section && toOpen.name) {
                            options.openAdminTool = _.pick(toOpen, 'section', 'name');
                        }
                        App.attachTo('#app', options);
                        _.defer(function() {
                            // Cache login in case server goes down
                            require(['login']);
                        });
                    }
                });
            }
        }
    }

    function isPopoutUrl(url) {
        return F.vertexUrl.isFullscreenUrl(url);
    }

    function isAddUrl(url) {
        return /#add=/.test(url);
    }

    function isMainApp(url) {
        return /#\s*$/.test(url) || url.indexOf('#') === -1;
    }

});
