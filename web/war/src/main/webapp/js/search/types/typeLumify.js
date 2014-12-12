define([
    'flight/lib/component',
    './withSearch',
    'util/formatters'
], function(
    defineComponent,
    withSearch,
    F) {
    'use strict';

    return defineComponent(SearchTypeLumify, withSearch);

    function SearchTypeLumify() {

        this.defaultAttrs({
            infiniteScrolling: true
        });

        this.after('initialize', function() {
            this.on('filterschange', function(event, data) {
                data.setAsteriskSearchOnEmpty = true;
            })
            this.on('querysubmit', this.onQuerySubmit);
            this.on('clearSearch', this.onClearSearch);
            this.on('infiniteScrollRequest', this.onInfiniteScrollRequest);
        });

        this.onClearSearch = function() {
            if (this.currentRequest && this.currentRequest.cancel) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }
        };

        this.onQuerySubmit = function(event, data) {
            var self = this,
                entityFilters = data.filters.entityFilters,
                relatedToVertexId = entityFilters && entityFilters.relatedToVertexId,
                query = relatedToVertexId ?  {
                        query: data.value,
                        relatedToVertexId: relatedToVertexId
                    } : data.value;

            this.currentQuery = data.value;
            this.currentFilters = data.filters;
            this.trigger('searchRequestBegan');
            this.triggerRequest(
                query,
                this.currentFilters.propertyFilters,
                this.currentFilters.conceptFilter,
                { offset: 0 }
            )
                .then(function(result) {
                    var unknownTotal = false;
                    if (!('totalHits' in result)) {
                        unknownTotal = true;
                        result.totalHits = result.vertices.length;
                    }

                    self.trigger('searchRequestCompleted', {
                        success: true,
                        result: result,
                        message: i18n('search.types.lumify.hits.' +
                            (
                                unknownTotal && result.totalHits >= (result.nextOffset - 1) ? 'unknown' :
                                result.totalHits === 0 ? 'none' :
                                result.totalHits === 1 ? 'one' :
                                'many'
                            ),
                            F.number.prettyApproximate(result.totalHits))
                    });
                }, function() {
                    self.trigger('searchRequestCompleted', { success: false, error: i18n('search.query.invalid') });
                })
                .done()
        };

        this.triggerRequest = function(query, propertyFilters, conceptFilter, paging) {
            if (this.currentRequest && this.currentRequest.cancel) {
                this.currentRequest.cancel();
                this.currentRequest = null;
            }

            return (
                this.currentRequest = this.dataRequest.apply(
                    this,
                    ['vertex', 'search'].concat([{
                        query: query,
                        propertyFilters: propertyFilters,
                        conceptFilter: conceptFilter,
                        paging: paging
                    }])
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
                .then(function(results) {
                    trigger({
                        success: true,
                        vertices: results.vertices,
                        total: results.totalHits,
                        nextOffset: results.nextOffset
                    });
                })
                .catch(function() {
                    trigger({ success: false });
                })
        };

    }
});
