define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeWorkspace, withSearch);

    function SearchTypeWorkspace() {

        this.before('initialize', function(node, config) {
            config.supportsHistogram = true;
        });

        this.after('initialize', function() {
            this.on('queryupdated', this.onQueryUpdated);
            this.on('workspaceFiltered', this.onWorkspaceFiltered);
            this.on(document, 'clearWorkspaceFilter', this.onClearWorkspaceFilter);
        });

        this.onClearWorkspaceFilter = function() {
            this.trigger('clearSearch');
        };

        this.onQueryUpdated = function(event, data) {
            this.trigger('searchRequestBegan');
            this.trigger('filterWorkspace', data);
        };

        this.onWorkspaceFiltered = function(event, data) {
            this.trigger('searchRequestCompleted', { success: true });
        };

    }
});
