define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeWorkspace, withSearch);

    function SearchTypeWorkspace() {

        this.before('initialize', function() {
            this.after('render', this.afterRender);
        });

        this.afterRender = function() {
            this.$node.find('.search-results-summary').text('Workspace Search');
        };

    }
});
