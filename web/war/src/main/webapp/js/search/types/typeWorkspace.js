define([
    'flight/lib/component',
    './withSearch',
    'util/formatters'
], function(
    defineComponent,
    withSearch,
    F) {
    'use strict';

    return defineComponent(SearchTypeWorkspace, withSearch);

    function SearchTypeWorkspace() {

        this.before('initialize', function(node, config) {
            config.supportsHistogram = true;
        });

        this.after('initialize', function() {
            this.on('queryupdated', this.onQueryUpdated);
            this.on(document, 'workspaceFiltered', this.onWorkspaceFiltered);
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
            this.trigger('searchRequestCompleted', {
                success: true,
                message: i18n('search.types.workspace.message',
                             F.number.prettyApproximate(data.hits),
                             F.number.prettyApproximate(data.total))
            });
        };

    }
});
