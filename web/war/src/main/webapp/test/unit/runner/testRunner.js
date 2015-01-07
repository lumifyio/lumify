var tests = Object.keys(window.__karma__.files).filter(function(file) {
    return (/^\/base\/test\/unit\/spec\/.*\.js$/).test(file);
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
            //sinon: '../libs/sinon/lib/sinon',
            //'sinon-chai': '../libs/sinon-chai/lib/sinon-chai',
            'mocha-flight': '../test/unit/utils/mocha-flight',

            // MOCKS
            'service/serviceBase': '../test/unit/mocks/serviceBase',
            'data/withServiceHandlers': '../test/unit/mocks/serviceHandlers',
            'util/service/messagesPromise': '../test/unit/mocks/messagePromise',
            'util/messages': '../test/unit/mocks/messages',
            testutils: '../test/unit/utils'
        },

        shim: {
            //sinon: { exports: 'sinon' }
        },

        deps: [
            'chai',
            //'sinon',
            '../libs/es5-shim/es5-shim',
            '../libs/es5-shim/es5-sham',
            '../libs/underscore/underscore',

            'util/handlebars/helpers'
        ],

        callback: function(chai, _sinon) {
            //sinon = _sinon;
            //sinon.spy = sinon.spy || {};

            require([
             //       'sinon-chai',
                    'timezone-js',
                    //'sinon/util/event',
                    //'sinon/util/fake_xml_http_request',
                    //'sinon/call',
                    //'sinon/stub',
                    //'sinon/spy',
                    //'sinon/mock',
                    'mocha-flight'
            ], function(/*sinonChai,*/timezoneJS) {

                chai.should();

                // Use sinon as mocking framework
                //chai.use(sinonChai);

                // Globals for assertions
                assert = chai.assert;
                expect = chai.expect;

                i18n = function(key) {
                    return key;
                };

                timezoneJS.timezone.zoneFileBasePath = '/base/tz';
                timezoneJS.timezone.init();

                // Use the twitter flight interface to mocha
                mocha.ui('mocha-flight');
                //mocha.setup('mocha-flight')
                mocha.options.globals.push('ejs', 'cytoscape', 'DEBUG');

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
