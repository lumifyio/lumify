define([
    'flight/lib/component',
    './withSearch'
], function(
    defineComponent,
    withSearch) {
    'use strict';

    return defineComponent(SearchTypeLumify, withSearch);

    function SearchTypeLumify() {

        this.defaultAttrs({
            infiniteScrolling: true
        });

        this.after('initialize', function() {
            this.on('querysubmit', this.onQuerySubmit);
            this.on('clearSearch', this.onClearSearch);
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);
        });

        this.onClearSearch = function() {
            if (this.currentRequest) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }
        };

        this.onQuerySubmit = function(event, data) {
            var self = this;

            this.currentQuery = data.value;
            this.currentFilters = data.filters;
            this.trigger('searchRequestBegan');
            this.triggerRequest(
                this.currentQuery,
                this.currentFilters.propertyFilters,
                this.currentFilters.conceptFilter,
                { offset: 0 }
            )
                .fail(function(error) {
                    self.trigger('searchRequestCompleted', { success: false, error: error });
                })
                .done(function(result) {
                    self.trigger('searchRequestCompleted', { success: true, result: result });
                });
        };

        this.triggerRequest = function() {
            if (this.currentRequest) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }

            return (
                this.currentRequest = this.serviceRequest.apply(
                    this,
                    ['vertex', 'graphVertexSearch'].concat(_.toArray(arguments))
                )
            );
        };

        this.getQueryForSubmitting = function() {
            if (this.currentFilters &&
                this.currentFilters.entityFilters &&
                this.currentFilters.entityFilters.relatedToVertexId) {
                return {
                    query: this.currentQuery,
                    relatedToVertexId: this.currentFilters.entityFilters.relatedToVertexId
                };
            }

            return this.currentQuery;
        };

        this.onInfiniteScrollRequest = function(event, data) {
            var query = this.getQueryForSubmitting(),
                trigger = this.trigger.bind(this,
                   this.select('resultsContainerSelector'),
                   'addInfiniteVertices'
                );

            this.triggerRequest(
                query,
                this.currentFilters.propertyFilters,
                this.currentFilters.conceptFilter,
                data.paging
            )
                .fail(function() {
                    trigger({ success: false });
                })
                .done(function(results) {
                    trigger({
                        success: true,
                        vertices: results.vertices,
                        total: results.totalHits,
                        nextOffset: results.nextOffset
                    });
                });
        };

    }
});
