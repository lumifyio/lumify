define(['jquery', 'underscore'], function($, _) {
    'use strict';

    var exporters = [{
        menuItem: 'PNG'
    }];

    return {
        exporters: exporters,

        registerWorkspaceExporter: function(exporter) {
            exporters.push(exporter);
        }
    };
});
