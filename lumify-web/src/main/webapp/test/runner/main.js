var tests = Object.keys(window.__karma__.files).filter(function (file) {
    return (/^\/base\/test\/spec\/.*\.js$/).test(file);
});

requirejs.config({
    shim: {
        '/base/js/require.config.js': { exports: 'require' }
    }
});
requirejs(['/base/js/require.config.js'], function(cfg) {

    var requireConfig = $.extend(true, {}, cfg, {

        // Karma serves files from '/base'
        baseUrl: '/base/js',

        paths: {
            chai: '../libs/chai/chai',
            sinon: '../libs/sinon/lib/sinon',
            'sinon-chai': '../libs/sinon-chai/lib/sinon-chai',
            'mocha-flight': '../libs/mocha-flight/lib/mocha-flight',

            // MOCKS
            'service/serviceBase': '../test/mocks/serviceBase'
        },

        shim: {
            sinon: { exports: 'sinon' }
        },

        deps: [ 
            'chai', 
            'sinon', 
            '../libs/es5-shim/es5-shim',
            '../libs/es5-shim/es5-sham',
            '../libs/underscore/underscore'
        ],

        callback: function(chai, sinon) {
            sinon.spy = sinon.spy || {};

            require([
                    'sinon-chai', 
                    'sinon/util/event',
                    'sinon/util/fake_xml_http_request',
                    'sinon/call',
                    'sinon/stub',
                    'sinon/spy',
                    'sinon/mock',
                    'mocha-flight'
            ], function(sinonChai) {

                // Use sinon as mocking framework
                chai.use(sinonChai);

                // Expose as global variables
                global.chai = chai;
                global.sinon = sinon;

                // Globals for assertions
                assert = chai.assert;
                expect = chai.expect;

                // Use the twitter flight interface to mocha
                mocha.ui('mocha-flight');
                mocha.options.globals.push( "ejs", "cytoscape", "DEBUG" );

                // Run tests after loading
                if (tests.length) {
                    require(tests, function() {
                        window.__karma__.start();
                    });
                } else window.__karma__.start();
            });

        }

    });
    requireConfig.deps = requireConfig.deps.concat(cfg.deps);
    delete requireConfig.urlArgs;

    window.require = requirejs;
    requirejs.config(requireConfig);
});


