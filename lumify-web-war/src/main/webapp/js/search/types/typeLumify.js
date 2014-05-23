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
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);
        });

        this.onQuerySubmit = function(event, data) {
            var self = this;

            this.currentQuery = data.value;
            this.trigger('searchRequestBegan');
            this.triggerRequest(this.currentQuery, [], null, { offset: 0 })
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

        this.onInfiniteScrollRequest = function(event, data) {
            var query = this.currentQuery,
                trigger = this.trigger.bind(this,
                   this.select('resultsContainerSelector'),
                   'addInfiniteVertices'
                );

            // TODO
            /*
            if (this.entityFilters && this.entityFilters.relatedToVertexId) {
                query = {
                    query: query,
                    relatedToVertexId: this.entityFilters.relatedToVertexId
                };
            }
            */

            this.triggerRequest(query, [], null, data.paging)
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
