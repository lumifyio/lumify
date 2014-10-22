define(['jquery', 'underscore'], function($, _) {
    'use strict';

    var PREFIX = 'WORKSPACE_EXPORT_',
        identifier = 0,
        exporters = [],
        byId = {};

    return {
        exporters: exporters,

        exportersById: byId,

        registerWorkspaceExporter: function(exporter) {
            exporter._identifier = PREFIX + identifier;
            byId[PREFIX + (identifier++)] = exporter;
            exporters.push(exporter);
        }
    };
});
