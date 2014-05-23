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
        });

        this.onQueryUpdated = function(event, data) {
            // console.log(data);
        };

    }
});
