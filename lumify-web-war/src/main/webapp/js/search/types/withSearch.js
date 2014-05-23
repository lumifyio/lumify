define([
    'flight/lib/registry',
    '../filters/filters',
    'hbs!./templates/type',
    'hbs!./templates/conceptSections',
    'util/withServiceRequest'
], function(
    registry,
    Filters,
    template,
    conceptsTemplate,
    withServiceRequest
) {
    'use strict';

    return withSearch;

    function withSearch() {

        withServiceRequest.call(this);

        this.defaultAttrs({
            resultsSelector: '.search-results',
            filtersSelector: '.search-filters',
            conceptsSelector: '.search-concepts'
        });

        this.after('initialize', function() {
            this.render();

            this.on('searchRequestBegan', this.onSearchResultsBegan);
            this.on('searchRequestCompleted', this.onSearchResultsCompleted);
            this.on('clearSearch', this.onClearSearch);
        });

        this.onClearSearch = function() {
            this.select('conceptsSelector').empty();
        };

        this.onSearchResultsBegan = function() {
            this.select('conceptsSelector').html(
                conceptsTemplate({ loading: true })
            );
        };

        this.onSearchResultsCompleted = function(event, data) {
            var self = this;

            if (data.success) {
                this.serviceRequest('ontology', 'concepts')
                    .done(function(concepts) {
                        self.select('conceptsSelector').html(
                            conceptsTemplate({
                                results: transform(concepts, data.results)
                            })
                        );
                    })
            } else {
                this.select('conceptsSelector').html(
                    conceptsTemplate({ error: data.error })
                );
            }
        };

        this.render = function() {
            this.$node.html(template({}));

            this.hideSearchResults();

            var filters = this.select('filtersSelector');
            Filters.attachTo(filters.find('.content'));
            this.makeResizable(filters);
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

    function transform(concepts, results) {
        if (concepts && results && results.verticesCount) {
            var counts = results.verticesCount,
                leafConcepts = _.chain(counts)
                    .keys()
                    .sortBy(function(k) {
                        return k.toLowerCase();
                    })
                    .value(),
                rolledUpCounts = leafConcepts.map(function(conceptId) {
                    return {
                        concept: concepts.byId[conceptId],
                        count: counts[conceptId]
                    };
                })

            leafConcepts.forEach(function(conceptId) {
                var concept = concepts.byId[conceptId],
                    parentConceptId = concept;

                while ((parentConceptId = parentConceptId.parentConcept)) {
                    if (!rolledUpCounts[parentConceptId]) {
                        rolledUpCounts[parentConceptId] = {
                            concept: concepts.byId[parentConceptId],
                            count: 0
                        }
                    }

                    rolledUpCounts[parentConceptId].count++;
                }
            });

            return rolledUpCounts;
        }
        return [];
    }
});
