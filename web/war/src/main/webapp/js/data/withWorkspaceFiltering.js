define([], function() {
    'use strict';

    return withWorkspaceFiltering;

    function withWorkspaceFiltering() {

        this.after('initialize', function() {
            this.on('filterWorkspace', this.onFilterWorkspace);
        });

        this.onFilterWorkspace = function(event, data) {
            this.worker.postMessage({
                type: 'workspaceFilter',
                data: data
            });
        };

    }
});
