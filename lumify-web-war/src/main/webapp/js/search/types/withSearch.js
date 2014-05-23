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
            this.hideSearchResults();
        };

        this.onSearchResultsBegan = function() {
            // TODO: start spinning badge
        };

        this.onSearchResultsCompleted = function(event, data) {
            debugger;
            if (data.success) {
                var self = this,
                    result = data.result,
                    vertices = result.vertices,
                    $searchResults = this.select('resultsSelector'),
                    $resultsContainer = $searchResults.find('.content > div')
                        .teardownComponent(VertexList)
                        .empty(),
                    $hits = $searchResults.find('.total-hits span').text(
                        'Found ' + F.string.plural(
                            F.number.pretty(result.totalHits),
                            'vertex', 'vertices'
                        )
                    );

                console.log('Hits: ', result.totalHits);

                VertexList.attachTo($resultsContainer, {
                    vertices: vertices,
                    infiniteScrolling: true,
                    //verticesConceptId: result.conceptId,
                    total: result.totalHits
                });
                this.makeResizable($searchResults);
                $searchResults.show().find('.multi-select');
                this.trigger(document, 'paneResized');
            } else {
                // TODO: show error
            }
        };

        this.render = function() {
            this.$node.html(template({}));

            this.hideSearchResults();

            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'));
        };

        this.hideSearchResults = function() {
            this.select('resultsSelector').hide()
                .find('.content > div').teardownComponent(VertexList).empty();
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
