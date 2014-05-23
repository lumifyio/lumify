define([
    'flight/lib/registry',
    '../filters/filters',
    'hbs!./templates/type',
    'util/withServiceRequest'
], function(
    registry,
    Filters,
    template,
    withServiceRequest
) {
    'use strict';

    return withSearch;

    function withSearch() {

        withServiceRequest.call(this);

        this.defaultAttrs({
            resultsSelector: '.search-results',
            filtersSelector: '.search-filters'
        });

        this.after('initialize', function() {
            this.render();

            this.on('searchRequestBegan', this.onSearchResultsBegan);
            this.on('searchRequestCompleted', this.onSearchResultsCompleted);
            this.on('clearSearch', this.onClearSearch);
        });

        this.onClearSearch = function() {
            // TODO: Rerender filters?
        };

        this.onSearchResultsBegan = function() {
            // TODO: start spinning badge
        };

        this.onSearchResultsCompleted = function(event, data) {
            var self = this;

            // TODO: show results
            /*
            if (data.success) {

            } else {

            }
            */
        };

        this.render = function() {
            this.$node.html(template({}));

            this.hideSearchResults();

            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'));
        };

        this.hideSearchResults = function() {
            registry.findInstanceInfoByNode(
                this.select('resultsSelector')
                    .hide()
                    .find('.content')[0]
            ).forEach(function(info) {
                info.instance.teardown();
            });
            this.trigger(document, 'paneResized');
        };

        this.makeResizable = function(node) {
            var self = this;

            // Add splitbar to search results
            return node.resizable({
                handles: 'e',
                minWidth: 200,
                maxWidth: 350,
                resize: function() {
                    self.trigger(document, 'paneResized');
                }
            });
        };

    }
});
