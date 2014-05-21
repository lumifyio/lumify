define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeLumify, withSearch);

    function SearchTypeLumify() {

        this.before('initialize', function() {
            this.after('render', this.afterRender);
        });

        this.after('initialize', function() {
            this.on('queryupdated', this.onQueryUpdated);
            this.on('querysubmit', this.onQuerySubmit);
        });

        this.onQueryUpdated = function(event, data) {
            console.log(data);
        };

        this.onQuerySubmit = function(event, data) {
            console.log('submit', data.value);
        };

        this.afterRender = function() {
            this.$node.find('.search-results-summary').text('Lumify Search');
        };
    }
});
