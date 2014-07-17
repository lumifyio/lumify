define([
    'service/serviceBase'
], function(ServiceBase) {
    'use strict';

    // Override in configuration.properties with `web.ui` prefix
    var DEFAULTS = {

        // Load related vertices thresholds
        'vertex.loadRelatedMaxBeforePrompt': 50,
        'vertex.loadRelatedMaxForceSearch': 250,

        // Hide multivalue properties after this count
        'properties.multivalue.defaultVisibleCount': 2,

        // Property Metadata shown in info popover
        'properties.metadata.propertyNames': 'http://lumify.io#sourceTimezone,' +
                                             'http://lumify.io#modifiedDate,' +
                                             'http://lumify.io#modifiedBy,' +
                                             'sandboxStatus,' +
                                             'http://lumify.io#confidence',
        'properties.metadata.propertyNamesDisplay': 'Original,' +
                                                    'Modified,' +
                                                    'By,' +
                                                    'Status,' +
                                                    'Confidence',
        'properties.metadata.propertyNamesType': 'timezone,' +
                                                 'datetime,' +
                                                 'user,' +
                                                 'sandboxStatus,' +
                                                 'percent',
        'map.provider': 'google',
        'map.provider.osm.url': 'https://a.tile.openstreetmap.org/${z}/${x}/${y}.png,' +
                                'https://b.tile.openstreetmap.org/${z}/${x}/${y}.png,' +
                                'https://c.tile.openstreetmap.org/${z}/${x}/${y}.png'
    };

    // Coerce all values to strings since that's what they will be from
    // server
    _.keys(DEFAULTS).forEach(function(key) {
        DEFAULTS[key] = '' + DEFAULTS[key];
    });

    function ConfigService() {
        ServiceBase.call(this);
        this.memoizeFunctions('config', ['getProperties']);
        return this;
    }

    ConfigService.prototype = Object.create(ServiceBase.prototype);

    ConfigService.prototype.getProperties = function() {
        return this._ajaxGet({ url: 'configuration' }).then(this.applyDefaults);
    };

    ConfigService.prototype.applyDefaults = function(properties) {
        return _.extend({}, DEFAULTS, properties);
    };

    return ConfigService;
});
