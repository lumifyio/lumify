
define([
    '../util/ajax',
    '../util/memoize'
], function(ajax, memoize) {
    'use strict';

    var DEFAULTS = {

            // Local cache rules for vertices / edges (per workspace)
            'cache.vertex.lru.expiration.seconds': (10 /*minutes*/) * 60,
            'cache.vertex.max_size': 500,
            'cache.edge.lru.expiration.seconds': (10 /*minutes*/) * 60,
            'cache.edge.max_size': 250,

            // Load related vertices thresholds
            'vertex.loadRelatedMaxBeforePrompt': 50,
            'vertex.loadRelatedMaxForceSearch': 250,

            'vertex.relationships.maxPerSection': 5,

            // Hide multivalue properties after this count
            'properties.multivalue.defaultVisibleCount': 2,

            // Property Metadata shown in info popover
            'properties.metadata.propertyNames': 'http://lumify.io#sourceTimezone,' +
                                                 'http://lumify.io#modifiedDate,' +
                                                 'http://lumify.io#modifiedBy,' +
                                                 'sandboxStatus,' +
                                                 'http://lumify.io#confidence',
            'properties.metadata.propertyNamesDisplay': 'properties.metadata.label.source_timezone,' +
                                                        'properties.metadata.label.modified_date,' +
                                                        'properties.metadata.label.modified_by,' +
                                                        'properties.metadata.label.status,' +
                                                        'properties.metadata.label.confidence',
            'properties.metadata.propertyNamesType': 'timezone,' +
                                                     'datetime,' +
                                                     'user,' +
                                                     'sandboxStatus,' +
                                                     'percent',
            'map.provider': 'google',
            'map.provider.osm.url': 'https://a.tile.openstreetmap.org/${z}/${x}/${y}.png,' +
                                    'https://b.tile.openstreetmap.org/${z}/${x}/${y}.png,' +
                                    'https://c.tile.openstreetmap.org/${z}/${x}/${y}.png'
        },
        getConfiguration = memoize(function() {
            var data = {};
            // TODO: move to main thread
            //try {
                //var language = localStorage.getItem('language');
                //if (language) {
                    //data.localeLanguage = language;
                //}
                //var country = localStorage.getItem('country');
                //if (country) {
                    //data.localeCountry = country;
                //}
                //var variant = localStorage.getItem('variant');
                //if (variant) {
                    //data.localeVariant = variant;
                //}
            //} catch(e) { console.log(e);}
            return ajax('GET', '/configuration', data);
        }),
        api = {
            properties: memoize(function() {
                return getConfiguration()
                    .then(_.property('properties'))
                    .then(function applyDefaults(properties) {
                        return _.extend({}, DEFAULTS, properties);
                    })
            }),

            messages: memoize(function() {
                return getConfiguration().then(_.property('messages'));
            })
        };

    return api;
});
