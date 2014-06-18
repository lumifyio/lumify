define([
    'flight/lib/registry',
    '../filters/filters',
    'hbs!./templates/type',
    'util/withServiceRequest',
    'util/formatters',
    'util/vertex/list'
], function(
    registry,
    Filters,
    template,
    withServiceRequest,
    F,
    VertexList
) {
    'use strict';

    return withSearch;

    function withSearch() {

        withServiceRequest.call(this);

        this.defaultAttrs({
            resultsSelector: '.search-results',
            resultsContainerSelector: '.search-results .content > div',
            filtersSelector: '.search-filters'
        });

        this.after('initialize', function() {
            this.render();

            this.on('searchRequestCompleted', function(event, data) {
                if (data.success && data.result) {
                    var self = this,
                        result = data.result,
                        vertices = result.vertices,
                        $searchResults = this.select('resultsSelector'),
                        $resultsContainer = this.select('resultsContainerSelector')
                            .teardownComponent(VertexList)
                            .empty(),
                        $hits = $searchResults.find('.total-hits').find('span').text(
                            'Found 0 results'
                        ).end().toggle(!result.totalHits);

                    VertexList.attachTo($resultsContainer, {
                        vertices: vertices,
                        nextOffset: result.nextOffset,
                        infiniteScrolling: this.attr.infiniteScrolling,
                        total: result.totalHits
                    });
                    this.makeResizable($searchResults);
                    $searchResults.show().find('.multi-select');
                    this.trigger(document, 'paneResized');
                }
            });
            this.on('clearSearch', function() {
                this.hideSearchResults();

                var filters = this.select('filtersSelector').find('.content')
                this.trigger(filters, 'clearfilters');
            });
        });

        this.render = function() {
            this.$node.html(template({}));

            this.hideSearchResults();

            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'), {
                supportsHistogram: this.attr.supportsHistogram === true
            });
        };

        this.hideSearchResults = function() {
            this.select('resultsSelector')
                .hide();
            this.select('resultsContainerSelector')
                .teardownComponent(VertexList)
                .empty();
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
