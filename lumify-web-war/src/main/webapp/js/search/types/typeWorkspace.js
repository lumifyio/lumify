define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeWorkspace, withSearch);

    function SearchTypeWorkspace() {

        this.after('initialize', function() {
            this.on('queryupdated', this.onQueryUpdated);
            this.on('workspaceFiltered', this.onWorkspaceFiltered);
        });

        this.onQueryUpdated = function(event, data) {
            this.trigger('searchRequestBegan');
            this.trigger('filterWorkspace', data);
        };

        this.onWorkspaceFiltered = function(event, data) {
            this.trigger('searchRequestCompleted', { success: true });
        };

    }
});
