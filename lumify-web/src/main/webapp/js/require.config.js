
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
];

var require = {
  baseUrl: '/js',
  urlArgs: "cache-bust=" +  Date.now(),
  paths: {
    'arbor': '../libs/cytoscape.js/lib/arbor',
    'colorjs': '../libs/color-js/color',
    'cytoscape': '../libs/cytoscape.js/build/cytoscape',
    'd3': '../libs/d3/d3',
    'ejs':  '../libs/ejs/ejs',
    'flight': '../libs/flight',
    'googlev3': '//maps.googleapis.com/maps/api/js?v=3&sensor=false&callback=googleV3Initialized',
    'intercom': '../libs/intercom/intercom',
    'map/map': 'map/map-openlayers',
    'openlayers': '../libs/openlayers/OpenLayers.debug',
    'pathfinding': '../libs/PathFinding.js/lib/pathfinding-browser',
    'sf': '../libs/sf/sf',
    'text': '../libs/requirejs-text/text',
    'three': '../libs/threejs/build/three.min',
    'tpl': '../libs/requirejs-ejs/rejs',
    'underscore': '../libs/underscore/underscore',
    'videojs': '../libs/video.js/video'
  },
  shim: {
	'underscore': { exports: '_' },
    'colorjs': { init: function() { return this.net.brehaut.Color; } },
    'cytoscape': { exports: 'cytoscape', deps:['arbor'] },
    'd3': { exports: 'd3' },
    'ejs': { exports: 'ejs' },
    'intercom': { exports:'Intercom' },
    'openlayers': { exports: 'OpenLayers', deps:['googlev3'] },
    'pathfinding': { exports: 'PF' },
    'three': { exports: 'THREE' },
    'videojs': { exports: 'videojs' }
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
