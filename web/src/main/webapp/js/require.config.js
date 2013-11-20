
var jQueryPlugins = {
  atmosphere: '../libs/jquery.atmosphere/jquery.atmosphere',
  withinScrollable: 'util/jquery.withinScrollable',
  flightJquery: 'util/jquery.flight',
  easing: '../libs/jquery.easing/js/jquery.easing',
  'bootstrap-datepicker': '../libs/bootstrap-datepicker/js/bootstrap-datepicker',
  removePrefixedClasses: 'util/jquery.removePrefixedClasses',
  scrollStop: '../libs/jquery-scrollstop/jquery.scrollstop'
};

var cytoscapePlugins = [
  'jquery.cytoscape-panzoom'
];

var require = {
  baseUrl: '/js',
  urlArgs: "cache-bust=" +  Date.now(),
  paths: {
    'flight': '../libs/flight',
    'text': '../libs/requirejs-text/text',
    'ejs':  '../libs/ejs/ejs',
    'tpl': '../libs/requirejs-ejs/rejs',
    'googlev3': '//maps.googleapis.com/maps/api/js?v=3&sensor=false&callback=googleV3Initialized',
    'map/map': 'map/map-openlayers',
    'openlayers': '../libs/openlayers/OpenLayers.debug',
    'cytoscape': '../libs/cytoscape.js/build/cytoscape',
    'arbor': '../libs/cytoscape.js/lib/arbor',
    'videojs': '../libs/video.js/video',
    'underscore': '../libs/underscore/underscore',
    'colorjs': '../libs/color-js/color',
    'sf': '../libs/sf/sf',
    'd3': '../libs/d3/d3',
    'three': '../libs/threejs/build/three.min',
    'intercom': '../libs/intercom/intercom'
  },
  shim: {
    'ejs': { exports: 'ejs' },
    'three': { exports: 'THREE' },
    'openlayers': { exports: 'OpenLayers', deps:['googlev3'] },
    'cytoscape': { exports: 'cytoscape', deps:['arbor'] },
    'colorjs': { init: function() { return this.net.brehaut.Color; } },
    'intercom': { exports:'Intercom' },
    'videojs': { exports: 'videojs' },
	'underscore': { exports: '_' },
    'd3': { exports: 'd3' }
  },
  deps : ['lumify']
};

Object.keys(jQueryPlugins).forEach(function(plugin) {
  require.paths[plugin] = jQueryPlugins[plugin];
  require.shim[plugin] = { exports: 'jQuery' };
});

cytoscapePlugins.forEach(function(plugin) {
  require.paths[plugin] = '../libs/cytoscape.js/build/plugins/' + plugin;
  require.shim[plugin] = { exports: 'jQuery' };
  require.shim.cytoscape.deps = require.shim.cytoscape.deps || [];
  require.shim.cytoscape.deps.push(plugin);
});


// For testing to use this configuration test/runner/main.js
if ('define' in window) {
    define([], function() {
        return require;
    });
}
